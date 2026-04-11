package biz.thonbecker.personal.skatetricks.platform;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class VideoFrameExtractor {

    private static final int DEFAULT_MAX_ANALYSIS_FRAMES = 24;
    private static final int MIN_ANALYSIS_FRAMES = 12;
    private static final double LONG_CLIP_SECONDS = 8.0;
    private static final double NORMAL_CLIP_SECONDS = 4.0;
    private static final double LONG_CLIP_EDGE_CONTEXT_RATIO = 0.08;
    private static final double LONG_CLIP_ACTION_WINDOW_START_RATIO = 0.18;
    private static final double LONG_CLIP_ACTION_WINDOW_END_RATIO = 0.82;

    @Value("${skatetricks.analysis.max-frames:24}")
    private int maxAnalysisFrames = DEFAULT_MAX_ANALYSIS_FRAMES;

    private final SkatetricksObservability observability;

    VideoFrameExtractor(SkatetricksObservability observability) {
        this.observability = observability;
    }

    List<String> extractBase64Frames(byte[] mp4VideoData) {
        final var scope = observability.start("frame_extractor.extract");
        if (mp4VideoData == null || mp4VideoData.length == 0) {
            observability.incrementStage("frame_extraction", "skipped", "reason", "empty_video");
            observability.success(scope, "reason", "empty_video");
            return List.of();
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(mp4VideoData);
        try (var channel = new ByteBufferSeekableByteChannel(byteBuffer, byteBuffer.remaining())) {
            FrameGrab frameGrab = FrameGrab.createFrameGrab(channel);
            var meta = frameGrab.getVideoTrack().getMeta();
            int totalFrames = Math.max(1, meta.getTotalFrames());
            double durationSeconds = meta.getTotalDuration();
            List<Integer> frameNumbers = sampleFrameNumbers(totalFrames, durationSeconds, maxAnalysisFrames);

            List<String> frames = new ArrayList<>(frameNumbers.size());
            for (int frameNumber : frameNumbers) {
                frameGrab.seekToFramePrecise(frameNumber);
                Picture picture = frameGrab.getNativeFrame();
                if (picture == null) {
                    continue;
                }
                frames.add(toBase64Jpeg(picture));
            }
            log.info(
                    "event=frame_extraction_completed inputBytes={} sampledFrames={} durationSeconds={}",
                    mp4VideoData.length,
                    frames.size(),
                    durationSeconds);
            observability.recordFrameCount(frames.size(), "mode", "video");
            observability.incrementStage("frame_extraction", "success");
            observability.success(scope);
            return frames;
        } catch (Exception e) {
            log.warn("event=frame_extraction_failed inputBytes={}", mp4VideoData.length, e);
            observability.incrementStage("frame_extraction", "failure");
            observability.failure(scope, e);
            return List.of();
        }
    }

    static List<Integer> sampleFrameNumbers(int totalFrames, double durationSeconds, int maxFrames) {
        int sanitizedTotalFrames = Math.max(1, totalFrames);
        int targetFrames = targetFrameCount(sanitizedTotalFrames, durationSeconds, maxFrames);
        if (sanitizedTotalFrames <= targetFrames) {
            List<Integer> frameNumbers = new ArrayList<>(sanitizedTotalFrames);
            for (int i = 0; i < sanitizedTotalFrames; i++) {
                frameNumbers.add(i);
            }
            return frameNumbers;
        }

        if (durationSeconds >= LONG_CLIP_SECONDS) {
            return sampleLongClipFrameNumbers(sanitizedTotalFrames, targetFrames);
        }
        if (durationSeconds >= NORMAL_CLIP_SECONDS) {
            return sampleNormalClipFrameNumbers(sanitizedTotalFrames, targetFrames);
        }
        return sampleEvenly(0, sanitizedTotalFrames - 1, targetFrames);
    }

    private static int targetFrameCount(int totalFrames, double durationSeconds, int maxFrames) {
        int sanitizedMaxFrames = Math.max(MIN_ANALYSIS_FRAMES, maxFrames);
        if (durationSeconds <= 0) {
            return Math.min(totalFrames, sanitizedMaxFrames);
        }

        int durationBasedTarget;
        if (durationSeconds < NORMAL_CLIP_SECONDS) {
            durationBasedTarget = (int) Math.ceil(durationSeconds * 6.0);
        } else if (durationSeconds < LONG_CLIP_SECONDS) {
            durationBasedTarget = (int) Math.ceil(durationSeconds * 3.0);
        } else {
            durationBasedTarget = sanitizedMaxFrames;
        }
        return Math.min(totalFrames, Math.min(sanitizedMaxFrames, Math.max(MIN_ANALYSIS_FRAMES, durationBasedTarget)));
    }

    private static List<Integer> sampleNormalClipFrameNumbers(int totalFrames, int targetFrames) {
        int actionStart = frameAtRatio(totalFrames, 0.10);
        int actionEnd = frameAtRatio(totalFrames, 0.90);

        Set<Integer> frameNumbers = new TreeSet<>();
        frameNumbers.add(0);
        frameNumbers.add(totalFrames - 1);
        addEvenly(frameNumbers, actionStart, actionEnd, targetFrames - frameNumbers.size());
        fillMissing(frameNumbers, actionStart, actionEnd, targetFrames);
        return List.copyOf(frameNumbers);
    }

    private static List<Integer> sampleLongClipFrameNumbers(int totalFrames, int targetFrames) {
        int earlyContext = frameAtRatio(totalFrames, LONG_CLIP_EDGE_CONTEXT_RATIO);
        int lateContext = frameAtRatio(totalFrames, 1.0 - LONG_CLIP_EDGE_CONTEXT_RATIO);
        int actionStart = frameAtRatio(totalFrames, LONG_CLIP_ACTION_WINDOW_START_RATIO);
        int actionEnd = frameAtRatio(totalFrames, LONG_CLIP_ACTION_WINDOW_END_RATIO);

        Set<Integer> frameNumbers = new TreeSet<>();
        frameNumbers.add(earlyContext);
        frameNumbers.add(lateContext);
        addEvenly(frameNumbers, actionStart, actionEnd, targetFrames - frameNumbers.size());
        fillMissing(frameNumbers, actionStart, actionEnd, targetFrames);
        return List.copyOf(frameNumbers);
    }

    private static List<Integer> sampleEvenly(int startFrame, int endFrame, int frameCount) {
        Set<Integer> frameNumbers = new TreeSet<>();
        addEvenly(frameNumbers, startFrame, endFrame, frameCount);
        return List.copyOf(frameNumbers);
    }

    private static void addEvenly(Set<Integer> frameNumbers, int startFrame, int endFrame, int frameCount) {
        if (frameCount <= 0) {
            return;
        }
        if (frameCount == 1) {
            frameNumbers.add(startFrame + Math.round((endFrame - startFrame) / 2.0f));
            return;
        }

        for (int i = 0; i < frameCount; i++) {
            int frameNumber = startFrame + Math.round((i * (endFrame - startFrame)) / (float) (frameCount - 1));
            frameNumbers.add(frameNumber);
        }
    }

    private static void fillMissing(Set<Integer> frameNumbers, int startFrame, int endFrame, int targetFrames) {
        int cursor = startFrame;
        while (frameNumbers.size() < targetFrames && cursor <= endFrame) {
            frameNumbers.add(cursor++);
        }
    }

    private static int frameAtRatio(int totalFrames, double ratio) {
        return Math.round((float) ((totalFrames - 1) * ratio));
    }

    private static String toBase64Jpeg(Picture picture) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(AWTUtil.toBufferedImage(picture), "jpg", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }
}
