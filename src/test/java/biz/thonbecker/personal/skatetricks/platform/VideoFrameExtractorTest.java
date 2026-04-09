package biz.thonbecker.personal.skatetricks.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class VideoFrameExtractorTest {

    @Test
    void shortClipsSampleDenselyAcrossEntireClip() {
        List<Integer> frames = VideoFrameExtractor.sampleFrameNumbers(90, 3.0, 24);

        assertEquals(18, frames.size());
        assertEquals(0, frames.getFirst());
        assertEquals(89, frames.getLast());
        assertChronological(frames);
    }

    @Test
    void normalClipsKeepContextAndSampleMainWindow() {
        List<Integer> frames = VideoFrameExtractor.sampleFrameNumbers(180, 6.0, 24);

        assertEquals(18, frames.size());
        assertEquals(0, frames.getFirst());
        assertEquals(179, frames.getLast());
        assertTrue(frames.contains(18), "should include start of trimmed action window");
        assertTrue(frames.contains(161), "should include end of trimmed action window");
        assertChronological(frames);
    }

    @Test
    void longClipsCapFrameCountAndAvoidExtremeIntroOutroFrames() {
        List<Integer> frames = VideoFrameExtractor.sampleFrameNumbers(900, 30.0, 24);

        assertEquals(24, frames.size());
        assertTrue(frames.getFirst() > 0, "long clips should not spend frames on the first frame");
        assertTrue(frames.getLast() < 899, "long clips should not spend frames on the last frame");
        assertTrue(frames.contains(72), "should retain early context near 8 percent");
        assertTrue(frames.contains(827), "should retain late context near 92 percent");
        assertTrue(frames.contains(162), "should include start of dense action window");
        assertTrue(frames.contains(737), "should include end of dense action window");
        assertChronological(frames);
    }

    @Test
    void unknownDurationFallsBackToConfiguredMaxFrames() {
        List<Integer> frames = VideoFrameExtractor.sampleFrameNumbers(300, 0.0, 24);

        assertEquals(24, frames.size());
        assertEquals(0, frames.getFirst());
        assertEquals(299, frames.getLast());
        assertChronological(frames);
    }

    @Test
    void tinyClipsUseEveryAvailableFrame() {
        List<Integer> frames = VideoFrameExtractor.sampleFrameNumbers(8, 1.0, 24);

        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), frames);
    }

    private static void assertChronological(List<Integer> frames) {
        for (int i = 1; i < frames.size(); i++) {
            assertTrue(frames.get(i) > frames.get(i - 1), "frames should be strictly increasing");
        }
    }
}
