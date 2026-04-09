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
            @org.springframework.lang.Nullable EmbeddingService embeddingService) {
        this.trickAnalyzer = trickAnalyzer;
        this.trickAttemptRepository = trickAttemptRepository;
        this.eventPublisher = eventPublisher;
        this.videoTranscoder = videoTranscoder;
        this.s3VectorsClient = s3VectorsClient;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public TrickAnalysisResult analyzeFrames(String sessionId, List<String> base64Frames) {
        log.info("Analyzing {} frames for session {}", base64Frames.size(), sessionId);

        TrickAnalysisResult result = trickAnalyzer.analyze(base64Frames);
        Long id = saveResult(sessionId, result);
        return result.withAttemptId(id);
    }

    @Transactional
    public TrickAnalysisResult analyzeVideo(String sessionId, byte[] videoData, String originalFilename) {
        log.info("Analyzing video for session {}: {} ({} bytes)", sessionId, originalFilename, videoData.length);

        try {
            byte[] mp4Data = videoTranscoder.convertToMp4(videoData, originalFilename).mp4Data();

            // Analyze with AI
            TrickAnalysisResult result = trickAnalyzer.analyzeVideo(mp4Data);
            Long id = saveResult(sessionId, result);
            return result.withAttemptId(id);

        } catch (VideoTranscodingException e) {
            log.error("Failed to process video for session {}", sessionId, e);
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
                    "Auto-verifying attempt for session {} (confidence {}% >= {}%)",
                    sessionId, result.confidence(), AUTO_VERIFY_CONFIDENCE_THRESHOLD);
        }

        entity = trickAttemptRepository.save(entity);

        if (entity.isVerified()) {
            writeToVectorStore(entity);
        }

        eventPublisher.publishEvent(new TrickAnalysisEvent(sessionId, result, Instant.now()));
        return entity.getId();
    }

    @Transactional
    public void verifyAttempt(Long attemptId, String correctedTrickName) {
        TrickAttemptEntity entity = trickAttemptRepository
                .findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        entity.setVerified(true);
        if (correctedTrickName != null && !correctedTrickName.isBlank()) {
            entity.setVerifiedTrickName(correctedTrickName);
            log.info("User corrected attempt {} from {} to {}", attemptId, entity.getTrickName(), correctedTrickName);
        } else {
            log.info(
                    "User confirmed attempt {} as {} ({}% confidence)",
                    attemptId, entity.getTrickName(), entity.getConfidence());
        }

        trickAttemptRepository.save(entity);
        writeToVectorStore(entity);
    }

    private void writeToVectorStore(final TrickAttemptEntity entity) {
        if (!vectorStoreEnabled || Objects.isNull(s3VectorsClient) || Objects.isNull(embeddingService)) {
            log.info("Vector store disabled, skipping write for attempt {}", entity.getId());
            return;
        }
        try {
            final var textToEmbed = Objects.nonNull(entity.getEmbeddingText())
                            && !entity.getEmbeddingText().isBlank()
                    ? entity.getEmbeddingText()
                    : entity.getPoseData();

            if (Objects.isNull(textToEmbed) || textToEmbed.isBlank()) {
                log.info(
                        "Skipping vector store write for attempt {} — no embedding or pose data available",
                        entity.getId());
                return;
            }

            final var acceptedTrick = Objects.nonNull(entity.getVerifiedTrickName())
                    ? entity.getVerifiedTrickName()
                    : entity.getTrickName();

            log.info(
                    "Writing attempt {} ({}) to vector store bucket={}, index={}",
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
            log.info("Calling S3 Vectors putVectors with key={}", vectorKey);

            s3VectorsClient.putVectors(r -> r.vectorBucketName(vectorBucket)
                    .indexName(vectorIndex)
                    .vectors(List.of(PutInputVector.builder()
                            .key(vectorKey)
                            .data(VectorData.fromFloat32(embedding))
                            .metadata(metadata)
                            .build())));

            log.info(
                    "✅ Successfully stored attempt {} ({}) in vector store '{}'",
                    entity.getId(),
                    acceptedTrick,
                    vectorIndex);
        } catch (Exception e) {
            log.error(
                    "❌ Failed to write attempt {} to vector store: {} - {}",
                    entity.getId(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            // Re-throw so user sees the error
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
        log.info("Converting video: {} ({} bytes)", originalFilename, videoData.length);

        try {
            return videoTranscoder.convertToMp4(videoData, originalFilename).mp4Data();

        } catch (VideoTranscodingException e) {
            log.error("Failed to convert video: {}", originalFilename, e);
            throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
        }
    }

    public VideoTranscoder.TranscodedVideo transcodeVideo(byte[] videoData, String originalFilename) {
        log.info("Transcoding video: {} ({} bytes)", originalFilename, videoData.length);

        try {
            return videoTranscoder.convertToMp4(videoData, originalFilename);
        } catch (VideoTranscodingException e) {
            log.error("Failed to transcode video: {}", originalFilename, e);
            throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
        }
    }

    public VideoTranscoder.TranscodedVideo transcodeUploadedVideo(String inputKey, String originalFilename) {
        log.info("Transcoding uploaded video from S3 key {} ({})", inputKey, originalFilename);

        try {
            return videoTranscoder.convertUploadedObjectToMp4(inputKey, originalFilename);
        } catch (VideoTranscodingException e) {
            log.error("Failed to transcode uploaded video: {} from {}", originalFilename, inputKey, e);
            throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TrickAnalysisResult analyzeConvertedVideo(String sessionId, byte[] mp4Data) {
        log.info("Analyzing converted video for session {} ({} bytes)", sessionId, mp4Data.length);

        try {
            TrickAnalysisResult result = trickAnalyzer.analyzeVideo(mp4Data);
            Long id = saveResult(sessionId, result);
            return result.withAttemptId(id);

        } catch (Exception e) {
            log.error("Failed to analyze video for session {}", sessionId, e);
            throw new RuntimeException("Video analysis failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TrickAnalysisResult analyzeConvertedVideo(String sessionId, String outputKey) {
        log.info("Analyzing converted video for session {} from output key {}", sessionId, outputKey);

        try {
            byte[] mp4Data = videoTranscoder.loadTranscodedVideo(outputKey);
            TrickAnalysisResult result = trickAnalyzer.analyzeVideo(mp4Data);
            Long id = saveResult(sessionId, result);
            return result.withAttemptId(id);

        } catch (VideoTranscodingException e) {
            log.error("Failed to load transcoded video for session {} from {}", sessionId, outputKey, e);
            throw new RuntimeException("Video analysis failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to analyze video for session {}", sessionId, e);
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
