package biz.thonbecker.personal.skatetricks.platform;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class VideoFrameExtractor {

    private static final int MAX_ANALYSIS_FRAMES = 10;

    List<String> extractBase64Frames(byte[] mp4VideoData) {
        if (mp4VideoData == null || mp4VideoData.length == 0) {
            return List.of();
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(mp4VideoData);
        try (var channel = new ByteBufferSeekableByteChannel(byteBuffer, byteBuffer.remaining())) {
            FrameGrab frameGrab = FrameGrab.createFrameGrab(channel);
            int totalFrames = Math.max(1, frameGrab.getVideoTrack().getMeta().getTotalFrames());
            Set<Integer> frameNumbers = sampleFrameNumbers(totalFrames, MAX_ANALYSIS_FRAMES);

            List<String> frames = new ArrayList<>(frameNumbers.size());
            for (int frameNumber : frameNumbers) {
                frameGrab.seekToFramePrecise(frameNumber);
                Picture picture = frameGrab.getNativeFrame();
                if (picture == null) {
                    continue;
                }
                frames.add(toBase64Jpeg(picture));
            }
            return frames;
        } catch (Exception e) {
            log.warn("Failed to extract video frames for analysis", e);
            return List.of();
        }
    }

    private static Set<Integer> sampleFrameNumbers(int totalFrames, int maxFrames) {
        Set<Integer> frameNumbers = new LinkedHashSet<>();
        if (totalFrames <= maxFrames) {
            for (int i = 0; i < totalFrames; i++) {
                frameNumbers.add(i);
            }
            return frameNumbers;
        }

        for (int i = 0; i < maxFrames; i++) {
            int frameNumber = Math.round((i * (totalFrames - 1)) / (float) (maxFrames - 1));
            frameNumbers.add(frameNumber);
        }
        return frameNumbers;
    }

    private static String toBase64Jpeg(Picture picture) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(AWTUtil.toBufferedImage(picture), "jpg", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }
}
