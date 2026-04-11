package biz.thonbecker.personal.skatetricks.platform.web;

import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import biz.thonbecker.personal.skatetricks.platform.RemoteVideoImportService;
import biz.thonbecker.personal.skatetricks.platform.SkateTricksService;
import biz.thonbecker.personal.skatetricks.platform.SkateTricksUploadService;
import biz.thonbecker.personal.skatetricks.platform.SkatetricksObservability;
import jakarta.annotation.PreDestroy;
import java.net.URI;
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
    private final RemoteVideoImportService remoteVideoImportService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SkatetricksObservability observability;

    @Value("${skatetricks.upload.max-file-size:500MB}")
    private DataSize maxUploadSize;

    // Converted videos stay in memory briefly for fast follow-up analysis; playback comes from the CDN URL.
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
            RemoteVideoImportService remoteVideoImportService,
            SimpMessagingTemplate messagingTemplate,
            SkatetricksObservability observability) {
        this.skateTricksService = skateTricksService;
        this.uploadService = uploadService;
        this.remoteVideoImportService = remoteVideoImportService;
        this.messagingTemplate = messagingTemplate;
        this.observability = observability;
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

    @PostMapping("/skatetricks/import-url")
    public ResponseEntity<ConvertResponse> importVideoUrl(@RequestBody ImportUrlRequest request) {
        if (request == null || isBlank(request.sessionId()) || isBlank(request.videoUrl())) {
            return ResponseEntity.badRequest().build();
        }

        String videoId = UUID.randomUUID().toString();
        conversionStatuses.put(videoId, new ConversionStatus("pending", 0, null, null, null, null));

        try {
            conversionExecutor.submit(() -> processRemoteVideoImport(videoId, request.sessionId(), request.videoUrl()));
        } catch (RejectedExecutionException e) {
            conversionStatuses.remove(videoId);
            return ResponseEntity.status(429).build();
        }

        return ResponseEntity.accepted().body(new ConvertResponse(videoId, 0, "pending"));
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

        conversionStatuses.put(request.videoId(), new ConversionStatus("pending", 0, null, null, null, null));

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
        final var scope = observability.start("controller.upload_conversion");
        try {
            updateConversionStatus(videoId, sessionId, "converting", 10, null, null, null);

            log.info(
                    "event=upload_conversion_started videoId={} sessionId={} inputKey={}",
                    videoId,
                    sessionId,
                    inputKey);
            var transcodedVideo = skateTricksService.transcodeUploadedVideo(inputKey, filename);
            byte[] mp4Data = transcodedVideo.mp4Data();
            observability.recordPayloadSize("converted_video", mp4Data.length, "source", "upload");

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

            updateConversionStatus(
                    videoId,
                    sessionId,
                    "complete",
                    100,
                    (long) mp4Data.length,
                    transcodedVideo.videoUrl(),
                    transcodedVideo.outputKey());
            observability.incrementStage("upload_conversion", "success");
            observability.success(scope, "source", "upload");
        } catch (Exception e) {
            log.error("event=upload_conversion_failed videoId={} sessionId={}", videoId, sessionId, e);
            String error = buildConversionErrorMessage(e);
            conversionStatuses.put(videoId, new ConversionStatus("error", 0, null, error, null, null));
            updateConversionStatus(videoId, sessionId, "error", 0, null, null, null, error);
            scheduleConversionStatusCleanup(videoId);
            observability.incrementStage("upload_conversion", "failure");
            observability.failure(scope, e, "source", "upload");
        }
    }

    private void processRemoteVideoImport(String videoId, String sessionId, String sourceUrl) {
        final var scope = observability.start("controller.remote_import_conversion");
        try {
            updateConversionStatus(videoId, sessionId, "converting", 5, null, null, null);
            var downloadedVideo = remoteVideoImportService.downloadVideo(sourceUrl, maxUploadSize.toBytes());
            observability.recordPayloadSize("remote_source_video", downloadedVideo.bytes().length, "source", "remote");

            updateConversionStatus(
                    videoId, sessionId, "converting", 35, (long) downloadedVideo.bytes().length, null, null);
            log.info(
                    "event=remote_import_transcode_started videoId={} sessionId={} host={} filename={} contentType={} size={}",
                    videoId,
                    sessionId,
                    URI.create(sourceUrl).getHost(),
                    downloadedVideo.filename(),
                    downloadedVideo.contentType(),
                    downloadedVideo.bytes().length);
            var transcodedVideo =
                    skateTricksService.transcodeVideo(downloadedVideo.bytes(), downloadedVideo.filename());

            convertedVideos.put(videoId, transcodedVideo.mp4Data());
            cleanupExecutor.schedule(
                    () -> {
                        convertedVideos.remove(videoId);
                        conversionStatuses.remove(videoId);
                        log.debug("Cleaned up imported video: {}", videoId);
                    },
                    STATUS_TTL_MINUTES,
                    TimeUnit.MINUTES);

            updateConversionStatus(
                    videoId,
                    sessionId,
                    "complete",
                    100,
                    (long) transcodedVideo.mp4Data().length,
                    transcodedVideo.videoUrl(),
                    transcodedVideo.outputKey());
            observability.incrementStage("remote_import_conversion", "success");
            observability.success(scope, "source", "remote");
        } catch (Exception e) {
            log.error(
                    "event=remote_import_transcode_failed videoId={} sessionId={} sourceUrl={}",
                    videoId,
                    sessionId,
                    sourceUrl,
                    e);
            String error = buildConversionErrorMessage(e);
            conversionStatuses.put(videoId, new ConversionStatus("error", 0, null, error, null, null));
            updateConversionStatus(videoId, sessionId, "error", 0, null, null, null, error);
            scheduleConversionStatusCleanup(videoId);
            observability.incrementStage("remote_import_conversion", "failure");
            observability.failure(scope, e, "source", "remote");
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
            String videoId,
            String sessionId,
            String status,
            int progress,
            Long size,
            String videoUrl,
            String outputKey) {
        updateConversionStatus(videoId, sessionId, status, progress, size, videoUrl, outputKey, null);
    }

    private void updateConversionStatus(
            String videoId,
            String sessionId,
            String status,
            int progress,
            Long size,
            String videoUrl,
            String outputKey,
            String error) {
        ConversionStatus convStatus = new ConversionStatus(status, progress, size, error, videoUrl, outputKey);
        conversionStatuses.put(videoId, convStatus);

        ConversionStatusUpdate update =
                new ConversionStatusUpdate(videoId, status, progress, size, videoUrl, outputKey, error);
        messagingTemplate.convertAndSend("/topic/skatetricks/conversion/" + sessionId, update);
    }

    @GetMapping("/skatetricks/convert/{videoId}/status")
    public ResponseEntity<ConversionStatusUpdate> getConversionStatus(@PathVariable String videoId) {
        ConversionStatus status = conversionStatuses.get(videoId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ConversionStatusUpdate(
                videoId,
                status.status(),
                status.progress(),
                status.size(),
                status.videoUrl(),
                status.outputKey(),
                status.error()));
    }

    @PostMapping("/skatetricks/analyze/{videoId}")
    public ResponseEntity<AnalysisResponse> analyzeVideo(
            @PathVariable String videoId, @RequestBody AnalyzeRequest request) {
        AnalyzeRequest enrichedRequest =
                new AnalyzeRequest(request != null ? request.sessionId() : null, videoId, null, null);
        return analyzeVideo(enrichedRequest);
    }

    @PostMapping("/skatetricks/analyze")
    public ResponseEntity<AnalysisResponse> analyzeVideo(@RequestBody AnalyzeRequest request) {
        if (request == null || isBlank(request.sessionId())) {
            return ResponseEntity.badRequest().build();
        }

        AnalysisSource analysisSource = resolveAnalysisSource(request);
        if (analysisSource == null) {
            return ResponseEntity.notFound().build();
        }

        String analysisId = UUID.randomUUID().toString();
        analysisStatuses.put(analysisId, new AnalysisStatus("pending", null, null));

        try {
            analysisExecutor.submit(() -> processAnalysis(
                    analysisId, request.sessionId(), analysisSource.videoData(), analysisSource.outputKey()));
        } catch (RejectedExecutionException e) {
            analysisStatuses.remove(analysisId);
            return ResponseEntity.status(429).build();
        }

        return ResponseEntity.accepted().body(new AnalysisResponse(analysisId, "pending"));
    }

    private void processAnalysis(String analysisId, String sessionId, byte[] videoData, String outputKey) {
        final var scope = observability.start("controller.analysis");
        try {
            analysisStatuses.put(analysisId, new AnalysisStatus("processing", null, null));
            log.info(
                    "event=analysis_started analysisId={} sessionId={} mode={} outputKey={}",
                    analysisId,
                    sessionId,
                    videoData != null ? "memory" : "cdn",
                    outputKey);
            TrickAnalysisResult result = videoData != null
                    ? skateTricksService.analyzeConvertedVideo(sessionId, videoData)
                    : skateTricksService.analyzeConvertedVideo(sessionId, outputKey);
            analysisStatuses.put(analysisId, new AnalysisStatus("complete", result, null));
            observability.incrementStage(
                    "analysis", "success", "trick", result.trick().name());
            observability.success(scope, "mode", videoData != null ? "memory" : "cdn");

            cleanupExecutor.schedule(
                    () -> {
                        analysisStatuses.remove(analysisId);
                        log.debug("Cleaned up analysis result: {}", analysisId);
                    },
                    STATUS_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error(
                    "event=analysis_failed analysisId={} sessionId={} outputKey={}",
                    analysisId,
                    sessionId,
                    outputKey,
                    e);
            analysisStatuses.put(analysisId, new AnalysisStatus("error", null, e.getMessage()));
            scheduleAnalysisStatusCleanup(analysisId);
            observability.incrementStage("analysis", "failure");
            observability.failure(scope, e, "mode", videoData != null ? "memory" : "cdn");
        }
    }

    private AnalysisSource resolveAnalysisSource(AnalyzeRequest request) {
        if (!isBlank(request.videoId())) {
            ConversionStatus conversionStatus = conversionStatuses.get(request.videoId());
            if (conversionStatus == null) {
                return null;
            }
            byte[] videoData = convertedVideos.get(request.videoId());
            String outputKey = firstNonBlank(
                    request.outputKey(), conversionStatus.outputKey(), deriveOutputKey(request.videoUrl()));
            if (videoData == null && isBlank(outputKey)) {
                return null;
            }
            return new AnalysisSource(videoData, outputKey);
        }

        String outputKey = firstNonBlank(request.outputKey(), deriveOutputKey(request.videoUrl()));
        if (isBlank(outputKey)) {
            return null;
        }
        return new AnalysisSource(null, outputKey);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String deriveOutputKey(String videoUrl) {
        if (isBlank(videoUrl)) {
            return null;
        }
        try {
            URI uri = URI.create(videoUrl);
            String path = uri.getPath();
            if (isBlank(path) || "/".equals(path)) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String buildConversionErrorMessage(Exception e) {
        Throwable root = rootCause(e);
        if (root instanceof RemoteVideoImportService.RemoteVideoImportException remoteImportException) {
            return remoteImportException.getMessage();
        }
        String message = root.getMessage();
        if (message != null && message.contains("MediaConvert")) {
            return "Video transcoding failed while converting the imported clip.";
        }
        if (message != null && message.contains("Video conversion failed:")) {
            return "Video conversion failed while preparing the clip for analysis.";
        }
        return "Video import failed. Check that the source URL is public and points to a supported video.";
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
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

    record ImportUrlRequest(String sessionId, String videoUrl) {}

    record UploadUrlResponse(String videoId, String inputKey, String uploadUrl, String contentType) {}

    record ConvertStartRequest(String videoId, String sessionId, String inputKey, String filename) {}

    record AnalyzeRequest(String sessionId, String videoId, String outputKey, String videoUrl) {}

    record ConvertResponse(String videoId, long size, String status) {}

    record UploadReservation(String inputKey, String filename, long size) {}

    record ConversionStatus(String status, int progress, Long size, String error, String videoUrl, String outputKey) {}

    record ConversionStatusUpdate(
            String videoId, String status, int progress, Long size, String videoUrl, String outputKey, String error) {}

    record AnalysisResponse(String analysisId, String status) {}

    record AnalysisStatus(String status, TrickAnalysisResult result, String error) {}

    record AnalysisStatusResponse(String status, TrickAnalysisResult result, String error) {}

    record AnalysisSource(byte[] videoData, String outputKey) {}

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
