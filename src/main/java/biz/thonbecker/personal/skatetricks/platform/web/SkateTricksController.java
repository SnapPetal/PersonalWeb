package biz.thonbecker.personal.skatetricks.platform.web;

import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.platform.SkateTricksService;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
class SkateTricksController {

    private final SkateTricksService skateTricksService;
    private final SimpMessagingTemplate messagingTemplate;

    // Temporary storage for converted videos (auto-expires after 10 minutes)
    private final Map<String, byte[]> convertedVideos = new ConcurrentHashMap<>();
    private final Map<String, ConversionStatus> conversionStatuses = new ConcurrentHashMap<>();
    private final Map<String, AnalysisStatus> analysisStatuses = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService conversionExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService analysisExecutor = Executors.newFixedThreadPool(2);

    SkateTricksController(SkateTricksService skateTricksService, SimpMessagingTemplate messagingTemplate) {
        this.skateTricksService = skateTricksService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/skatetricks")
    public String skateTricksPage() {
        return "skatetricks";
    }

    @PostMapping("/skatetricks/convert")
    public ResponseEntity<ConvertResponse> convertVideo(
            @RequestParam("video") MultipartFile video, @RequestParam("sessionId") String sessionId) {

        if (video.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Generate video ID immediately
        String videoId = UUID.randomUUID().toString();
        String filename = video.getOriginalFilename();
        long fileSize = video.getSize();

        try {
            // Copy video bytes before returning (MultipartFile may be cleaned up)
            byte[] videoBytes = video.getBytes();

            log.info(
                    "Queuing video conversion: {} ({} bytes) for session {}, videoId={}",
                    filename,
                    fileSize,
                    sessionId,
                    videoId);

            // Set initial status
            conversionStatuses.put(videoId, new ConversionStatus("pending", 0, null, null));

            // Return immediately with video ID, process async
            conversionExecutor.submit(() -> processVideoConversion(videoId, sessionId, videoBytes, filename));

            return ResponseEntity.accepted().body(new ConvertResponse(videoId, 0, "pending"));

        } catch (IOException e) {
            log.error("Failed to read uploaded video", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void processVideoConversion(String videoId, String sessionId, byte[] videoBytes, String filename) {
        try {
            // Send "converting" status
            updateConversionStatus(videoId, sessionId, "converting", 10, null);

            log.info("Starting conversion for videoId={}", videoId);
            byte[] mp4Data = skateTricksService.convertVideo(videoBytes, filename);

            // Store converted video
            convertedVideos.put(videoId, mp4Data);

            // Schedule cleanup after 10 minutes
            cleanupExecutor.schedule(
                    () -> {
                        convertedVideos.remove(videoId);
                        conversionStatuses.remove(videoId);
                        log.debug("Cleaned up converted video: {}", videoId);
                    },
                    10,
                    TimeUnit.MINUTES);

            log.info("Video converted successfully: {} -> {} bytes, id={}", filename, mp4Data.length, videoId);

            // Send "complete" status
            updateConversionStatus(videoId, sessionId, "complete", 100, (long) mp4Data.length);

        } catch (Exception e) {
            log.error("Failed to convert video: {}", videoId, e);
            updateConversionStatus(videoId, sessionId, "error", 0, null);
            conversionStatuses.put(videoId, new ConversionStatus("error", 0, null, e.getMessage()));
        }
    }

    private void updateConversionStatus(String videoId, String sessionId, String status, int progress, Long size) {
        ConversionStatus convStatus = new ConversionStatus(status, progress, size, null);
        conversionStatuses.put(videoId, convStatus);

        // Send WebSocket update
        ConversionStatusUpdate update = new ConversionStatusUpdate(videoId, status, progress, size);
        messagingTemplate.convertAndSend("/topic/skatetricks/conversion/" + sessionId, update);
        log.debug("Sent conversion status update: {} -> {}", videoId, status);
    }

    @GetMapping("/skatetricks/convert/{videoId}/status")
    public ResponseEntity<ConversionStatusUpdate> getConversionStatus(@PathVariable String videoId) {
        ConversionStatus status = conversionStatuses.get(videoId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(
                new ConversionStatusUpdate(videoId, status.status(), status.progress(), status.size()));
    }

    @GetMapping("/skatetricks/video/{videoId}")
    public ResponseEntity<byte[]> getConvertedVideo(@PathVariable String videoId) {
        log.info("Serving video: {} (stored videos: {})", videoId, convertedVideos.keySet());
        byte[] videoData = convertedVideos.get(videoId);

        if (videoData == null) {
            log.warn("Video not found: {}", videoId);
            return ResponseEntity.notFound().build();
        }

        log.info("Returning video {} ({} bytes)", videoId, videoData.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.valueOf("video/mp4").toString())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(videoData.length))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(videoData);
    }

    @PostMapping("/skatetricks/analyze/{videoId}")
    public ResponseEntity<AnalysisResponse> analyzeVideo(
            @PathVariable String videoId, @RequestParam("sessionId") String sessionId) {

        log.info("Analyze request for video: {} (stored videos: {})", videoId, convertedVideos.keySet());
        byte[] videoData = convertedVideos.get(videoId);

        if (videoData == null) {
            return ResponseEntity.notFound().build();
        }

        // Generate analysis ID
        String analysisId = UUID.randomUUID().toString();

        // Set initial status
        analysisStatuses.put(analysisId, new AnalysisStatus("pending", null, null));

        // Return immediately, process async
        analysisExecutor.submit(() -> processAnalysis(analysisId, sessionId, videoData, videoId));

        return ResponseEntity.accepted().body(new AnalysisResponse(analysisId, "pending"));
    }

    private void processAnalysis(String analysisId, String sessionId, byte[] videoData, String videoId) {
        try {
            log.info(
                    "Starting analysis for analysisId={}, videoId={} ({} bytes)",
                    analysisId,
                    videoId,
                    videoData.length);

            // Update status to processing
            analysisStatuses.put(analysisId, new AnalysisStatus("processing", null, null));

            TrickAnalysisResult result = skateTricksService.analyzeConvertedVideo(sessionId, videoData);

            log.info("Analysis complete for analysisId={}: {}", analysisId, result.trick());

            // Store result
            analysisStatuses.put(analysisId, new AnalysisStatus("complete", result, null));

            // Schedule cleanup after 10 minutes
            cleanupExecutor.schedule(
                    () -> {
                        analysisStatuses.remove(analysisId);
                        log.debug("Cleaned up analysis result: {}", analysisId);
                    },
                    10,
                    TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("Failed to analyze video: {}", analysisId, e);
            analysisStatuses.put(analysisId, new AnalysisStatus("error", null, e.getMessage()));
        }
    }

    @GetMapping("/skatetricks/analyze/{analysisId}/status")
    public ResponseEntity<AnalysisStatusResponse> getAnalysisStatus(@PathVariable String analysisId) {
        AnalysisStatus status = analysisStatuses.get(analysisId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new AnalysisStatusResponse(status.status(), status.result(), status.error()));
    }

    @PostMapping("/skatetricks/attempts/{id}/verify")
    public ResponseEntity<?> verifyAttempt(
            @PathVariable Long id, @RequestBody(required = false) VerifyRequest request) {
        try {
            String corrected = request != null ? request.correctedTrickName() : null;
            skateTricksService.verifyAttempt(id, corrected);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to verify attempt {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to verify: " + e.getMessage()));
        }
    }

    record ConvertResponse(String videoId, long size, String status) {}

    record ConversionStatus(String status, int progress, Long size, String error) {}

    record ConversionStatusUpdate(String videoId, String status, int progress, Long size) {}

    record AnalysisResponse(String analysisId, String status) {}

    record AnalysisStatus(String status, TrickAnalysisResult result, String error) {}

    record AnalysisStatusResponse(String status, TrickAnalysisResult result, String error) {}

    record VerifyRequest(String correctedTrickName) {}
}
