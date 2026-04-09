package biz.thonbecker.personal.skatetricks.platform.web;

import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.platform.SkateTricksService;
import biz.thonbecker.personal.skatetricks.platform.SkateTricksUploadService;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@Slf4j
class SkateTricksController {

    private static final int STATUS_TTL_MINUTES = 10;
    private static final int WORKER_THREADS = 2;
    private static final int MAX_QUEUE_DEPTH = 20;

    private final SkateTricksService skateTricksService;
    private final SkateTricksUploadService uploadService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${skatetricks.upload.max-file-size:500MB}")
    private DataSize maxUploadSize;

    // Converted videos stay in memory briefly for analysis; playback comes from the CDN URL.
    private final Map<String, byte[]> convertedVideos = new ConcurrentHashMap<>();
    private final Map<String, UploadReservation> uploadReservations = new ConcurrentHashMap<>();
    private final Map<String, ConversionStatus> conversionStatuses = new ConcurrentHashMap<>();
    private final Map<String, AnalysisStatus> analysisStatuses = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService conversionExecutor = new ThreadPoolExecutor(
            WORKER_THREADS,
            WORKER_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUE_DEPTH),
            new ThreadPoolExecutor.AbortPolicy());
    private final ExecutorService analysisExecutor = new ThreadPoolExecutor(
            WORKER_THREADS,
            WORKER_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUE_DEPTH),
            new ThreadPoolExecutor.AbortPolicy());

    SkateTricksController(
            SkateTricksService skateTricksService,
            SkateTricksUploadService uploadService,
            SimpMessagingTemplate messagingTemplate) {
        this.skateTricksService = skateTricksService;
        this.uploadService = uploadService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/skatetricks")
    public String skateTricksPage() {
        return "skatetricks";
    }

    @PostMapping("/skatetricks/upload-url")
    public ResponseEntity<UploadUrlResponse> createUploadUrl(@RequestBody UploadUrlRequest request) {
        if (request == null || isBlank(request.filename()) || request.size() == null || request.size() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        if (request.size() > maxUploadSize.toBytes()) {
            return ResponseEntity.status(413).build();
        }

        String videoId = UUID.randomUUID().toString();
        var upload = uploadService.createPresignedUpload(request.filename(), request.contentType());
        uploadReservations.put(videoId, new UploadReservation(upload.inputKey(), request.filename(), request.size()));
        scheduleUploadReservationCleanup(videoId);

        return ResponseEntity.ok(
                new UploadUrlResponse(videoId, upload.inputKey(), upload.uploadUrl(), upload.contentType()));
    }

    @PostMapping("/skatetricks/convert")
    public ResponseEntity<ConvertResponse> startConversion(@RequestBody ConvertStartRequest request) {
        if (request == null
                || isBlank(request.videoId())
                || isBlank(request.inputKey())
                || isBlank(request.filename())
                || isBlank(request.sessionId())) {
            return ResponseEntity.badRequest().build();
        }

        UploadReservation reservation = uploadReservations.get(request.videoId());
        if (reservation == null || !reservation.inputKey().equals(request.inputKey())) {
            return ResponseEntity.notFound().build();
        }

        conversionStatuses.put(request.videoId(), new ConversionStatus("pending", 0, null, null, null));

        try {
            conversionExecutor.submit(() -> processVideoConversion(
                    request.videoId(), request.sessionId(), request.inputKey(), request.filename()));
        } catch (RejectedExecutionException e) {
            conversionStatuses.remove(request.videoId());
            return ResponseEntity.status(429).build();
        }

        return ResponseEntity.accepted().body(new ConvertResponse(request.videoId(), 0, "pending"));
    }

    private void processVideoConversion(String videoId, String sessionId, String inputKey, String filename) {
        try {
            updateConversionStatus(videoId, sessionId, "converting", 10, null, null);

            log.info("Starting conversion for videoId={} from inputKey={}", videoId, inputKey);
            var transcodedVideo = skateTricksService.transcodeUploadedVideo(inputKey, filename);
            byte[] mp4Data = transcodedVideo.mp4Data();

            convertedVideos.put(videoId, mp4Data);
            uploadReservations.remove(videoId);

            cleanupExecutor.schedule(
                    () -> {
                        convertedVideos.remove(videoId);
                        conversionStatuses.remove(videoId);
                        log.debug("Cleaned up converted video: {}", videoId);
                    },
                    STATUS_TTL_MINUTES,
                    TimeUnit.MINUTES);

            updateConversionStatus(videoId, sessionId, "complete", 100, (long) mp4Data.length, transcodedVideo.videoUrl());
        } catch (Exception e) {
            log.error("Failed to convert video: {}", videoId, e);
            conversionStatuses.put(videoId, new ConversionStatus("error", 0, null, e.getMessage(), null));
            updateConversionStatus(videoId, sessionId, "error", 0, null, null);
            scheduleConversionStatusCleanup(videoId);
        }
    }

    private void scheduleUploadReservationCleanup(String videoId) {
        cleanupExecutor.schedule(
                () -> {
                    uploadReservations.remove(videoId);
                    log.debug("Cleaned up upload reservation: {}", videoId);
                },
                STATUS_TTL_MINUTES,
                TimeUnit.MINUTES);
    }

    private void scheduleConversionStatusCleanup(String videoId) {
        cleanupExecutor.schedule(
                () -> {
                    conversionStatuses.remove(videoId);
                    log.debug("Cleaned up conversion status: {}", videoId);
                },
                STATUS_TTL_MINUTES,
                TimeUnit.MINUTES);
    }

    private void updateConversionStatus(
            String videoId, String sessionId, String status, int progress, Long size, String videoUrl) {
        ConversionStatus convStatus = new ConversionStatus(status, progress, size, null, videoUrl);
        conversionStatuses.put(videoId, convStatus);

        ConversionStatusUpdate update = new ConversionStatusUpdate(videoId, status, progress, size, videoUrl);
        messagingTemplate.convertAndSend("/topic/skatetricks/conversion/" + sessionId, update);
    }

    @GetMapping("/skatetricks/convert/{videoId}/status")
    public ResponseEntity<ConversionStatusUpdate> getConversionStatus(@PathVariable String videoId) {
        ConversionStatus status = conversionStatuses.get(videoId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(
                new ConversionStatusUpdate(videoId, status.status(), status.progress(), status.size(), status.videoUrl()));
    }

    @PostMapping("/skatetricks/analyze/{videoId}")
    public ResponseEntity<AnalysisResponse> analyzeVideo(
            @PathVariable String videoId, @RequestBody AnalyzeRequest request) {
        if (request == null || isBlank(request.sessionId())) {
            return ResponseEntity.badRequest().build();
        }

        byte[] videoData = convertedVideos.get(videoId);
        if (videoData == null) {
            return ResponseEntity.notFound().build();
        }

        String analysisId = UUID.randomUUID().toString();
        analysisStatuses.put(analysisId, new AnalysisStatus("pending", null, null));

        try {
            analysisExecutor.submit(() -> processAnalysis(analysisId, request.sessionId(), videoData, videoId));
        } catch (RejectedExecutionException e) {
            analysisStatuses.remove(analysisId);
            return ResponseEntity.status(429).build();
        }

        return ResponseEntity.accepted().body(new AnalysisResponse(analysisId, "pending"));
    }

    private void processAnalysis(String analysisId, String sessionId, byte[] videoData, String videoId) {
        try {
            analysisStatuses.put(analysisId, new AnalysisStatus("processing", null, null));
            TrickAnalysisResult result = skateTricksService.analyzeConvertedVideo(sessionId, videoData);
            analysisStatuses.put(analysisId, new AnalysisStatus("complete", result, null));

            cleanupExecutor.schedule(
                    () -> {
                        analysisStatuses.remove(analysisId);
                        log.debug("Cleaned up analysis result: {}", analysisId);
                    },
                    STATUS_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to analyze video: {}", analysisId, e);
            analysisStatuses.put(analysisId, new AnalysisStatus("error", null, e.getMessage()));
            scheduleAnalysisStatusCleanup(analysisId);
        }
    }

    private void scheduleAnalysisStatusCleanup(String analysisId) {
        cleanupExecutor.schedule(
                () -> {
                    analysisStatuses.remove(analysisId);
                    log.debug("Cleaned up analysis status: {}", analysisId);
                },
                STATUS_TTL_MINUTES,
                TimeUnit.MINUTES);
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

    record UploadUrlRequest(String filename, String contentType, Long size) {}

    record UploadUrlResponse(String videoId, String inputKey, String uploadUrl, String contentType) {}

    record ConvertStartRequest(String videoId, String sessionId, String inputKey, String filename) {}

    record AnalyzeRequest(String sessionId) {}

    record ConvertResponse(String videoId, long size, String status) {}

    record UploadReservation(String inputKey, String filename, long size) {}

    record ConversionStatus(String status, int progress, Long size, String error, String videoUrl) {}

    record ConversionStatusUpdate(String videoId, String status, int progress, Long size, String videoUrl) {}

    record AnalysisResponse(String analysisId, String status) {}

    record AnalysisStatus(String status, TrickAnalysisResult result, String error) {}

    record AnalysisStatusResponse(String status, TrickAnalysisResult result, String error) {}

    record VerifyRequest(String correctedTrickName) {}

    @PreDestroy
    void shutdownExecutors() {
        conversionExecutor.shutdown();
        analysisExecutor.shutdown();
        cleanupExecutor.shutdown();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
