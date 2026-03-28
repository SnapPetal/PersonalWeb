package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisEvent;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.api.TrickSequenceEntry;
import biz.thonbecker.personal.skatetricks.domain.TrickCatalog;
import biz.thonbecker.personal.skatetricks.platform.FFmpegVideoConverter.VideoConversionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final FFmpegVideoConverter videoConverter;
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
            FFmpegVideoConverter videoConverter,
            @org.springframework.lang.Nullable S3VectorsClient s3VectorsClient,
            @org.springframework.lang.Nullable EmbeddingService embeddingService) {
        this.trickAnalyzer = trickAnalyzer;
        this.trickAttemptRepository = trickAttemptRepository;
        this.eventPublisher = eventPublisher;
        this.videoConverter = videoConverter;
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

        Path inputPath = null;
        Path convertedPath = null;

        try {
            // Save uploaded video to temp file
            String extension = getFileExtension(originalFilename);
            inputPath = Files.createTempFile("upload-", extension);
            Files.write(inputPath, videoData);

            // Convert to MP4 if needed
            byte[] mp4Data;
            if (extension.equalsIgnoreCase(".mp4")) {
                mp4Data = videoData;
                log.info("Video is already MP4, skipping conversion");
            } else {
                log.info("Converting {} to MP4", extension);
                convertedPath = videoConverter.convertToMp4(inputPath);
                mp4Data = Files.readAllBytes(convertedPath);
            }

            // Analyze with AI
            TrickAnalysisResult result = trickAnalyzer.analyzeVideo(mp4Data);
            Long id = saveResult(sessionId, result);
            return result.withAttemptId(id);

        } catch (IOException | VideoConversionException e) {
            log.error("Failed to process video for session {}", sessionId, e);
            throw new RuntimeException("Video processing failed: " + e.getMessage(), e);
        } finally {
            // Clean up temp files
            deleteQuietly(inputPath);
            deleteQuietly(convertedPath);
        }
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
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

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path, e);
            }
        }
    }

    public byte[] convertVideo(byte[] videoData, String originalFilename) {
        log.info("Converting video: {} ({} bytes)", originalFilename, videoData.length);

        Path inputPath = null;
        Path convertedPath = null;

        try {
            String extension = getFileExtension(originalFilename);

            // If already MP4, return as-is
            if (extension.equalsIgnoreCase(".mp4")) {
                log.info("Video is already MP4, skipping conversion");
                return videoData;
            }

            // Save to temp file and convert
            inputPath = Files.createTempFile("upload-", extension);
            Files.write(inputPath, videoData);

            log.info("Converting {} to MP4", extension);
            convertedPath = videoConverter.convertToMp4(inputPath);
            return Files.readAllBytes(convertedPath);

        } catch (IOException | VideoConversionException e) {
            log.error("Failed to convert video: {}", originalFilename, e);
            throw new RuntimeException("Video conversion failed: " + e.getMessage(), e);
        } finally {
            deleteQuietly(inputPath);
            deleteQuietly(convertedPath);
        }
    }

    @Transactional
    public TrickAnalysisResult analyzeConvertedVideo(String sessionId, byte[] mp4Data) {
        log.info("Analyzing converted video for session {} ({} bytes)", sessionId, mp4Data.length);

        Path tempPath = null;
        try {
            // Save MP4 to temp file for frame extraction
            tempPath = Files.createTempFile("analyze-", ".mp4");
            Files.write(tempPath, mp4Data);

            // Extract frames using FFmpeg
            List<String> frames = videoConverter.extractFrames(tempPath);
            log.info("Extracted {} frames for analysis", frames.size());

            // Analyze frames with AI
            TrickAnalysisResult result = trickAnalyzer.analyze(frames);
            Long id = saveResult(sessionId, result);
            return result.withAttemptId(id);

        } catch (IOException | VideoConversionException e) {
            log.error("Failed to analyze video for session {}", sessionId, e);
            throw new RuntimeException("Video analysis failed: " + e.getMessage(), e);
        } finally {
            deleteQuietly(tempPath);
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
