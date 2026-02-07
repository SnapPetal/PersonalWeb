package biz.thonbecker.personal.skatetricks.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class FFmpegVideoConverter {

    private static final int TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_FRAME_COUNT = 30;

    // Paketo apt buildpack installs packages here
    private static final String APT_LAYER = "/layers/paketo-buildpacks_apt/apt";

    /**
     * Configures ProcessBuilder with correct environment for apt buildpack installations.
     * This adds the apt layer paths to PATH and LD_LIBRARY_PATH so ffmpeg can find its libraries.
     */
    private void configureEnvironment(ProcessBuilder pb) {
        var env = pb.environment();

        // Check if running in buildpack environment (apt layer exists)
        if (Files.isDirectory(Path.of(APT_LAYER))) {
            // Add apt layer bin to PATH
            String aptBin = APT_LAYER + "/usr/bin";
            String currentPath = env.getOrDefault("PATH", "/usr/bin:/bin");
            env.put("PATH", aptBin + ":" + currentPath);

            // Add apt layer libs to LD_LIBRARY_PATH including pulseaudio subdirectory
            String aptLibs = String.join(
                    ":",
                    APT_LAYER + "/usr/lib/x86_64-linux-gnu",
                    APT_LAYER + "/usr/lib/x86_64-linux-gnu/pulseaudio",
                    APT_LAYER + "/lib/x86_64-linux-gnu",
                    APT_LAYER + "/usr/lib");
            String currentLdPath = env.getOrDefault("LD_LIBRARY_PATH", "");
            env.put("LD_LIBRARY_PATH", aptLibs + (currentLdPath.isEmpty() ? "" : ":" + currentLdPath));

            log.debug(
                    "Configured apt buildpack environment: PATH={}, LD_LIBRARY_PATH={}",
                    env.get("PATH"),
                    env.get("LD_LIBRARY_PATH"));
        }
    }

    /**
     * Converts a video file to MP4 format using FFmpeg.
     *
     * @param inputPath path to the input video file
     * @return path to the converted MP4 file (caller must delete after use)
     * @throws VideoConversionException if conversion fails
     */
    public Path convertToMp4(Path inputPath) throws VideoConversionException {
        try {
            Path outputPath = Files.createTempFile("converted-", ".mp4");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i",
                    inputPath.toString(),
                    "-c:v",
                    "libx264",
                    "-preset",
                    "fast",
                    "-crf",
                    "23",
                    "-c:a",
                    "aac",
                    "-movflags",
                    "+faststart",
                    "-y", // overwrite output
                    outputPath.toString());

            configureEnvironment(pb);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                Files.deleteIfExists(outputPath);
                throw new VideoConversionException("FFmpeg conversion timed out after " + TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                Files.deleteIfExists(outputPath);
                log.error("FFmpeg failed with exit code {}: {}", exitCode, output);
                throw new VideoConversionException("FFmpeg conversion failed with exit code " + exitCode);
            }

            log.info(
                    "Converted video to MP4: {} -> {} ({} bytes)",
                    inputPath.getFileName(),
                    outputPath.getFileName(),
                    Files.size(outputPath));

            return outputPath;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoConversionException("Video conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts frames from a video file as base64-encoded JPEG images.
     *
     * @param inputPath path to the input video file
     * @param frameCount number of frames to extract (evenly distributed across video)
     * @return list of base64-encoded JPEG frames
     * @throws VideoConversionException if extraction fails
     */
    public List<String> extractFrames(Path inputPath, int frameCount) throws VideoConversionException {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("frames-");

            // First, get video duration
            double duration = getVideoDuration(inputPath);
            log.info("Video duration: {} seconds", duration);

            // Calculate interval to distribute frames across entire video
            // Using select filter to pick frames at regular intervals
            double interval = Math.max(0.1, duration / frameCount);
            String selectFilter = String.format(
                    "select='isnan(prev_selected_t)+gte(t-prev_selected_t\\,%.3f)',scale=640:-1", interval);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i",
                    inputPath.toString(),
                    "-vf",
                    selectFilter,
                    "-vsync",
                    "vfr",
                    "-vframes",
                    String.valueOf(frameCount),
                    "-q:v",
                    "5",
                    "-y",
                    tempDir.resolve("frame%03d.jpg").toString());

            configureEnvironment(pb);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new VideoConversionException("FFmpeg frame extraction timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg frame extraction failed with exit code {}: {}", exitCode, output);
                throw new VideoConversionException("FFmpeg frame extraction failed with exit code " + exitCode);
            }

            // Read extracted frames
            List<String> frames = new ArrayList<>();
            for (int i = 1; i <= frameCount; i++) {
                Path framePath = tempDir.resolve(String.format("frame%03d.jpg", i));
                if (Files.exists(framePath)) {
                    byte[] frameData = Files.readAllBytes(framePath);
                    frames.add(Base64.getEncoder().encodeToString(frameData));
                    Files.delete(framePath);
                }
            }

            log.info("Extracted {} frames from video", frames.size());
            return frames;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoConversionException("Frame extraction failed: " + e.getMessage(), e);
        } finally {
            // Clean up temp directory
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir);
                } catch (IOException e) {
                    log.warn("Failed to delete temp directory: {}", tempDir, e);
                }
            }
        }
    }

    /**
     * Extracts frames from a video file using default frame count.
     */
    public List<String> extractFrames(Path inputPath) throws VideoConversionException {
        return extractFrames(inputPath, DEFAULT_FRAME_COUNT);
    }

    /**
     * Gets the duration of a video file in seconds using ffprobe.
     */
    private double getVideoDuration(Path inputPath) throws VideoConversionException {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v",
                    "error",
                    "-show_entries",
                    "format=duration",
                    "-of",
                    "default=noprint_wrappers=1:nokey=1",
                    inputPath.toString());

            configureEnvironment(pb);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes()).trim();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (!finished || process.exitValue() != 0) {
                log.warn("Could not get video duration, defaulting to 10 seconds");
                return 10.0;
            }

            return Double.parseDouble(output);
        } catch (Exception e) {
            log.warn("Error getting video duration: {}, defaulting to 10 seconds", e.getMessage());
            return 10.0;
        }
    }

    /**
     * Checks if FFmpeg is available on the system.
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            configureEnvironment(pb);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    public static class VideoConversionException extends Exception {
        public VideoConversionException(String message) {
            super(message);
        }

        public VideoConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
