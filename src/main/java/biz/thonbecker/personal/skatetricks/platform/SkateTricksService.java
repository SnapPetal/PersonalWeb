package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisEvent;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.api.TrickSequenceEntry;
import biz.thonbecker.personal.skatetricks.domain.TrickCatalog;
import biz.thonbecker.personal.skatetricks.platform.VideoTranscoder.VideoTranscodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.model.PutInputVector;
import software.amazon.awssdk.services.s3vectors.model.VectorData;

@Service
@Slf4j
public class SkateTricksService {

    private final TrickAnalyzer trickAnalyzer;
    private final TrickAttemptRepository trickAttemptRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VideoTranscoder videoTranscoder;
    private final S3VectorsClient s3VectorsClient;
    private final EmbeddingService embeddingService;
    private final SkatetricksObservability observability;

    @Value("${skatetricks.vectorstore.bucket:}")
    private String vectorBucket;

    @Value("${skatetricks.vectorstore.index:}")
    private String vectorIndex;

    @Value("${skatetricks.vectorstore.enabled:true}")
    private boolean vectorStoreEnabled;

    SkateTricksService(
            TrickAnalyzer trickAnalyzer,
            TrickAttemptRepository trickAttemptRepository,
            ApplicationEventPublisher eventPublisher,
            VideoTranscoder videoTranscoder,
            @org.springframework.lang.Nullable S3VectorsClient s3VectorsClient,
            @org.springframework.lang.Nullable EmbeddingService embeddingService,
            SkatetricksObservability observability) {
        this.trickAnalyzer = trickAnalyzer;
        this.trickAttemptRepository = trickAttemptRepository;
        this.eventPublisher = eventPublisher;
        this.videoTranscoder = videoTranscoder;
        this.s3VectorsClient = s3VectorsClient;
        this.embeddingService = embeddingService;
        this.observability = observability;
    }

    @Transactional
    public TrickAnalysisResult analyzeFrames(String sessionId, List<String> base64Frames) {
        final var scope = observability.start("service.analyze_frames");
        log.info("event=analyze_frames_started sessionId={} frameCount={}", sessionId, base64Frames.size());
        observability.recordFrameCount(base64Frames.size(), "mode", "frames");

        try {
            TrickAnalysisResult result = trickAnalyzer.analyze(base64Frames);
            Long id = saveResult(sessionId, result);
            observability.success(scope, "trick", result.trick().name());
            return result.withAttemptId(id);
        } catch (RuntimeException e) {
            observability.failure(scope, e, "mode", "frames");
            throw e;
        }
    }

    @Transactional
    public TrickAnalysisResult analyzeVideo(String sessionId, byte[] videoData, String originalFilename) {
        final var scope = observability.start("service.analyze_video_upload");
        log.info(
                "event=analyze_video_started sessionId={} filename={} bytes={}",
                sessionId,
                originalFilename,
                videoData.length);
        observability.recordPayloadSize("input_video", videoData.length, "source", "upload");

        try {
            byte[] mp4Data =
                    videoTranscoder.convertToMp4(videoData, originalFilename).mp4Data();
            observability.recordPayloadSize("converted_video", mp4Data.length, "source", "upload");

            TrickAnalysisResult result = trickAnalyzer.analyzeVideo(mp4Data);
            Long id = saveResult(sessionId, result);
            observability.success(scope, "trick", result.trick().name());
            return result.withAttemptId(id);

        } catch (VideoTranscodingException e) {
            log.error("event=analyze_video_failed sessionId={} filename={}", sessionId, originalFilename, e);
            observability.failure(scope, e, "source", "upload");
            throw new RuntimeException("Video processing failed: " + e.getMessage(), e);
        }
    }

    private static final int AUTO_VERIFY_CONFIDENCE_THRESHOLD = 80;

    private Long saveResult(String sessionId, TrickAnalysisResult result) {
        TrickAttemptEntity entity = new TrickAttemptEntity();
        entity.setSessionId(sessionId);
        entity.setTrickName(result.trick().name());
        entity.setConfidence(result.confidence());
        entity.setFormScore(result.formScore());
        entity.setFeedback(Objects.nonNull(result.feedback()) ? String.join("|", result.feedback()) : "");
        entity.setTrickSequence(encodeTrickSequence(result.trickSequence()));
        entity.setPoseData(result.poseData());
        entity.setEmbeddingText(result.embeddingText());

        if (result.confidence() >= AUTO_VERIFY_CONFIDENCE_THRESHOLD) {
            entity.setVerified(true);
            log.info(
                    "event=attempt_auto_verified sessionId={} confidence={} threshold={}",
                    sessionId,
                    result.confidence(),
                    AUTO_VERIFY_CONFIDENCE_THRESHOLD);
        }

        entity = trickAttemptRepository.save(entity);
        log.info(
                "event=attempt_saved sessionId={} attemptId={} trick={} confidence={} verified={}",
                sessionId,
                entity.getId(),
                entity.getTrickName(),
                entity.getConfidence(),
                entity.isVerified());

        if (entity.isVerified()) {
            writeToVectorStore(entity);
        }

        eventPublisher.publishEvent(new TrickAnalysisEvent(sessionId, result, Instant.now()));
        return entity.getId();
    }

    @Transactional
    public void verifyAttempt(Long attemptId, String correctedTrickName) {
        final var scope = observability.start("service.verify_attempt");
        TrickAttemptEntity entity = trickAttemptRepository
                .findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        entity.setVerified(true);
        if (correctedTrickName != null && !correctedTrickName.isBlank()) {
            entity.setVerifiedTrickName(correctedTrickName);
            log.info(
                    "event=attempt_corrected attemptId={} originalTrick={} correctedTrick={}",
                    attemptId,
                    entity.getTrickName(),
                    correctedTrickName);
        } else {
            log.info(
                    "event=attempt_confirmed attemptId={} trick={} confidence={}",
                    attemptId,
                    entity.getTrickName(),
                    entity.getConfidence());
        }

        trickAttemptRepository.save(entity);
        writeToVectorStore(entity);
        observability.incrementStage("attempt_verification", "success");
        observability.success(scope);
    }

    private void writeToVectorStore(final TrickAttemptEntity entity) {
        final var scope = observability.start("service.write_vector_store");
        if (!vectorStoreEnabled || Objects.isNull(s3VectorsClient) || Objects.isNull(embeddingService)) {
            log.info("event=vector_store_skipped attemptId={} reason=disabled", entity.getId());
            observability.incrementStage("vector_store_write", "skipped", "reason", "disabled");
            observability.success(scope, "reason", "disabled");
            return;
        }
        try {
            final var textToEmbed = Objects.nonNull(entity.getEmbeddingText())
                            && !entity.getEmbeddingText().isBlank()
                    ? entity.getEmbeddingText()
                    : entity.getPoseData();

            if (Objects.isNull(textToEmbed) || textToEmbed.isBlank()) {
                log.info("event=vector_store_skipped attemptId={} reason=no_embedding_source", entity.getId());
                observability.incrementStage("vector_store_write", "skipped", "reason", "no_embedding_source");
                observability.success(scope, "reason", "no_embedding_source");
                return;
            }

            final var acceptedTrick = Objects.nonNull(entity.getVerifiedTrickName())
                    ? entity.getVerifiedTrickName()
                    : entity.getTrickName();

            log.info(
                    "event=vector_store_write_started attemptId={} trick={} bucket={} index={}",
                    entity.getId(),
                    acceptedTrick,
                    vectorBucket,
                    vectorIndex);

            log.debug("Embedding text (length={})", textToEmbed.length());
            final var embedding = embeddingService.embed(textToEmbed);
            log.debug("Generated embedding with {} dimensions", embedding.size());

            final var metadata = Document.fromMap(Map.of(
                    "trickName", Document.fromString(acceptedTrick),
                    "confidence", Document.fromNumber(entity.getConfidence()),
                    "formScore", Document.fromNumber(entity.getFormScore()),
                    "attemptId", Document.fromNumber(entity.getId()),
                    "feedback",
                            Document.fromString(Objects.nonNull(entity.getFeedback()) ? entity.getFeedback() : "")));

            final var vectorKey = "attempt-" + entity.getId();
            log.info("event=vector_store_put_vectors attemptId={} key={}", entity.getId(), vectorKey);

            s3VectorsClient.putVectors(r -> r.vectorBucketName(vectorBucket)
                    .indexName(vectorIndex)
                    .vectors(List.of(PutInputVector.builder()
                            .key(vectorKey)
                            .data(VectorData.fromFloat32(embedding))
                            .metadata(metadata)
                            .build())));

            log.info(
                    "event=vector_store_write_completed attemptId={} trick={} index={}",
                    entity.getId(),
                    acceptedTrick,
                    vectorIndex);
            observability.incrementStage("vector_store_write", "success");
            observability.success(scope, "trick", acceptedTrick);
        } catch (Exception e) {
            log.error(
                    "event=vector_store_write_failed attemptId={} errorType={} errorMessage={}",
                    entity.getId(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            observability.incrementStage(
                    "vector_store_write", "failure", "error", e.getClass().getSimpleName());
            observability.failure(scope, e);
            throw new RuntimeException("Failed to write to vector store: " + e.getMessage(), e);
        }
    }

    private String encodeTrickSequence(List<TrickSequenceEntry> sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return null;
        }
        return sequence.stream()
                .map(e -> "%s:%s:%d".formatted(e.trick().name(), e.timeframe(), e.confidence()))
                .reduce((a, b) -> a + "|" + b)
                .orElse(null);
    }

    private List<TrickSequenceEntry> decodeTrickSequence(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        List<TrickSequenceEntry> result = new ArrayList<>();
        for (String part : encoded.split("\\|")) {
            String[] fields = part.split(":", 3);
            if (fields.length == 3) {
                SupportedTrick trick = TrickCatalog.fromName(fields[0]);
                String timeframe = fields[1];
                int confidence = 0;
                try {
                    confidence = Integer.parseInt(fields[2]);
                } catch (NumberFormatException ignored) {
                }
                result.add(new TrickSequenceEntry(trick, timeframe, confidence));
            }
        }
        return result;
    }

    public byte[] convertVideo(byte[] videoData, String originalFilename) {
        final var scope = observability.start("service.convert_video");
        log.info("event=convert_video_started filename={} bytes={}", originalFilename, videoData.length);
        observability.recordPayloadSize("input_video", videoData.length, "source", "remote");

        try {
            final var transcodedVideo = videoTranscoder.convertToMp4(videoData, originalFilename);
            observability.recordPayloadSize("converted_video", transcodedVideo.mp4Data().length, "source", "remote");
            observability.success(scope, "source", "remote");
            return transcodedVideo.mp4Data();

        } catch (VideoTranscodingException e) {
            log.error("event=convert_video_failed filename={}", originalFilename, e);
            observability.failure(scope, e, "source", "remote");
            throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
        }
    }

    public VideoTranscoder.TranscodedVideo transcodeVideo(byte[] videoData, String originalFilename) {
        final var scope = observability.start("service.transcode_video");
        log.info("event=transcode_video_started filename={} bytes={}", originalFilename, videoData.length);
        observability.recordPayloadSize("input_video", videoData.length, "source", "remote");

        try {
            final var transcodedVideo = videoTranscoder.convertToMp4(videoData, originalFilename);
            observability.recordPayloadSize("converted_video", transcodedVideo.mp4Data().length, "source", "remote");
            observability.success(scope, "source", "remote");
            return transcodedVideo;
        } catch (VideoTranscodingException e) {
            log.error("event=transcode_video_failed filename={}", originalFilename, e);
            observability.failure(scope, e, "source", "remote");
            throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
        }
    }

    public VideoTranscoder.TranscodedVideo transcodeUploadedVideo(String inputKey, String originalFilename) {
        final var scope = observability.start("service.transcode_uploaded_video");
        log.info("event=transcode_uploaded_video_started inputKey={} filename={}", inputKey, originalFilename);

        try {
            final var transcodedVideo = videoTranscoder.convertUploadedObjectToMp4(inputKey, originalFilename);
            observability.recordPayloadSize("converted_video", transcodedVideo.mp4Data().length, "source", "upload");
            observability.success(scope, "source", "upload");
            return transcodedVideo;
        } catch (VideoTranscodingException e) {
            log.error("event=transcode_uploaded_video_failed inputKey={} filename={}", inputKey, originalFilename, e);
            observability.failure(scope, e, "source", "upload");
            throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TrickAnalysisResult analyzeConvertedVideo(String sessionId, byte[] mp4Data) {
        final var scope = observability.start("service.analyze_converted_video");
        log.info("event=analyze_converted_video_started sessionId={} bytes={}", sessionId, mp4Data.length);
        observability.recordPayloadSize("converted_video", mp4Data.length, "mode", "memory");

        try {
            TrickAnalysisResult result = trickAnalyzer.analyzeVideo(mp4Data);
            Long id = saveResult(sessionId, result);
            observability.success(scope, "trick", result.trick().name(), "mode", "memory");
            return result.withAttemptId(id);

        } catch (Exception e) {
            log.error("event=analyze_converted_video_failed sessionId={} mode=memory", sessionId, e);
            observability.failure(scope, e, "mode", "memory");
            throw new RuntimeException("Video analysis failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TrickAnalysisResult analyzeConvertedVideo(String sessionId, String outputKey) {
        final var scope = observability.start("service.analyze_cdn_video");
        log.info("event=analyze_cdn_video_started sessionId={} outputKey={}", sessionId, outputKey);

        try {
            byte[] mp4Data = videoTranscoder.loadTranscodedVideo(outputKey);
            observability.recordPayloadSize("converted_video", mp4Data.length, "mode", "cdn");
            TrickAnalysisResult result = trickAnalyzer.analyzeVideo(mp4Data);
            Long id = saveResult(sessionId, result);
            observability.success(scope, "trick", result.trick().name(), "mode", "cdn");
            return result.withAttemptId(id);

        } catch (VideoTranscodingException e) {
            log.error("event=analyze_cdn_video_load_failed sessionId={} outputKey={}", sessionId, outputKey, e);
            observability.failure(scope, e, "mode", "cdn");
            throw new RuntimeException("Video analysis failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("event=analyze_cdn_video_failed sessionId={} outputKey={}", sessionId, outputKey, e);
            observability.failure(scope, e, "mode", "cdn");
            throw new RuntimeException("Video analysis failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<TrickAnalysisResult> getSessionHistory(String sessionId) {
        return trickAttemptRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(e -> new TrickAnalysisResult(
                        TrickCatalog.fromName(e.getTrickName()),
                        e.getConfidence(),
                        e.getFormScore(),
                        e.getFeedback() != null ? List.of(e.getFeedback().split("\\|")) : List.of(),
                        decodeTrickSequence(e.getTrickSequence()),
                        e.getPoseData(),
                        e.getEmbeddingText(),
                        e.getId()))
                .toList();
    }
}
