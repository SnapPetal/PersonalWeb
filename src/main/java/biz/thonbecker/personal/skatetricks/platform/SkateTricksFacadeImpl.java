package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SkateTricksFacade;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisEvent;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.domain.TrickCatalog;
import biz.thonbecker.personal.skatetricks.platform.FFmpegVideoConverter.VideoConversionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
class SkateTricksFacadeImpl implements SkateTricksFacade {

    private final TrickAnalyzer trickAnalyzer;
    private final TrickAttemptRepository trickAttemptRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FFmpegVideoConverter videoConverter;

    SkateTricksFacadeImpl(
            TrickAnalyzer trickAnalyzer,
            TrickAttemptRepository trickAttemptRepository,
            ApplicationEventPublisher eventPublisher,
            FFmpegVideoConverter videoConverter) {
        this.trickAnalyzer = trickAnalyzer;
        this.trickAttemptRepository = trickAttemptRepository;
        this.eventPublisher = eventPublisher;
        this.videoConverter = videoConverter;
    }

    @Override
    @Transactional
    public TrickAnalysisResult analyzeFrames(String sessionId, List<String> base64Frames) {
        log.info("Analyzing {} frames for session {}", base64Frames.size(), sessionId);

        TrickAnalysisResult result = trickAnalyzer.analyze(base64Frames);

        TrickAttemptEntity entity = new TrickAttemptEntity();
        entity.setSessionId(sessionId);
        entity.setTrickName(result.trick().name());
        entity.setConfidence(result.confidence());
        entity.setFormScore(result.formScore());
        entity.setFeedback(String.join("|", result.feedback()));
        trickAttemptRepository.save(entity);

        eventPublisher.publishEvent(new TrickAnalysisEvent(sessionId, result, Instant.now()));

        return result;
    }

    @Override
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

            // Save to database
            TrickAttemptEntity entity = new TrickAttemptEntity();
            entity.setSessionId(sessionId);
            entity.setTrickName(result.trick().name());
            entity.setConfidence(result.confidence());
            entity.setFormScore(result.formScore());
            entity.setFeedback(String.join("|", result.feedback()));
            trickAttemptRepository.save(entity);

            eventPublisher.publishEvent(new TrickAnalysisEvent(sessionId, result, Instant.now()));

            return result;

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

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path, e);
            }
        }
    }

    @Override
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

    @Override
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

            // Save to database
            TrickAttemptEntity entity = new TrickAttemptEntity();
            entity.setSessionId(sessionId);
            entity.setTrickName(result.trick().name());
            entity.setConfidence(result.confidence());
            entity.setFormScore(result.formScore());
            entity.setFeedback(String.join("|", result.feedback()));
            trickAttemptRepository.save(entity);

            eventPublisher.publishEvent(new TrickAnalysisEvent(sessionId, result, Instant.now()));

            return result;

        } catch (IOException | VideoConversionException e) {
            log.error("Failed to analyze video for session {}", sessionId, e);
            throw new RuntimeException("Video analysis failed: " + e.getMessage(), e);
        } finally {
            deleteQuietly(tempPath);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrickAnalysisResult> getSessionHistory(String sessionId) {
        return trickAttemptRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(e -> new TrickAnalysisResult(
                        TrickCatalog.fromName(e.getTrickName()),
                        e.getConfidence(),
                        e.getFormScore(),
                        e.getFeedback() != null ? List.of(e.getFeedback().split("\\|")) : List.of()))
                .toList();
    }
}
