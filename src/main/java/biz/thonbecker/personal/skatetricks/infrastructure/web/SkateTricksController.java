package biz.thonbecker.personal.skatetricks.infrastructure.web;

import biz.thonbecker.personal.skatetricks.api.SkateTricksFacade;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
class SkateTricksController {

    private final SkateTricksFacade skateTricksFacade;

    // Temporary storage for converted videos (auto-expires after 10 minutes)
    private final Map<String, byte[]> convertedVideos = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    SkateTricksController(SkateTricksFacade skateTricksFacade) {
        this.skateTricksFacade = skateTricksFacade;
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

        try {
            log.info(
                    "Converting video: {} ({} bytes) for session {}",
                    video.getOriginalFilename(),
                    video.getSize(),
                    sessionId);

            byte[] mp4Data = skateTricksFacade.convertVideo(video.getBytes(), video.getOriginalFilename());

            // Store converted video with unique ID
            String videoId = UUID.randomUUID().toString();
            convertedVideos.put(videoId, mp4Data);

            // Schedule cleanup after 10 minutes
            cleanupExecutor.schedule(
                    () -> {
                        convertedVideos.remove(videoId);
                        log.debug("Cleaned up converted video: {}", videoId);
                    },
                    10,
                    TimeUnit.MINUTES);

            log.info(
                    "Video converted successfully: {} -> {} bytes, id={}",
                    video.getOriginalFilename(),
                    mp4Data.length,
                    videoId);

            return ResponseEntity.ok(new ConvertResponse(videoId, mp4Data.length));

        } catch (IOException e) {
            log.error("Failed to read uploaded video", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Failed to convert video", e);
            return ResponseEntity.internalServerError().build();
        }
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
    public ResponseEntity<TrickAnalysisResult> analyzeVideo(
            @PathVariable String videoId, @RequestParam("sessionId") String sessionId) {

        log.info("Analyze request for video: {} (stored videos: {})", videoId, convertedVideos.keySet());
        byte[] videoData = convertedVideos.get(videoId);

        if (videoData == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            log.info("Analyzing converted video: {} ({} bytes) for session {}", videoId, videoData.length, sessionId);

            TrickAnalysisResult result = skateTricksFacade.analyzeConvertedVideo(sessionId, videoData);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to analyze video", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    record ConvertResponse(String videoId, long size) {}
}
