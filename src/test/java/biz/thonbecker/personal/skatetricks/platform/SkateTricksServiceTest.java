package biz.thonbecker.personal.skatetricks.platform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import biz.thonbecker.personal.skatetricks.api.TrickAnalysisResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class SkateTricksServiceTest {

    @Test
    void convertVideoDelegatesToConfiguredTranscoder() {
        byte[] input = {1, 2, 3};
        byte[] converted = {9, 8, 7};
        AtomicReference<String> filenameRef = new AtomicReference<>();
        AtomicReference<byte[]> inputRef = new AtomicReference<>();

        VideoTranscoder transcoder = new VideoTranscoder() {
            @Override
            public VideoTranscoder.TranscodedVideo convertToMp4(byte[] videoData, String originalFilename) {
                inputRef.set(videoData);
                filenameRef.set(originalFilename);
                return new VideoTranscoder.TranscodedVideo(
                        converted, "https://cdn.example.com/video.mp4", "skatetricks/output/test/video.mp4");
            }

            @Override
            public VideoTranscoder.TranscodedVideo convertUploadedObjectToMp4(
                    String inputKey, String originalFilename) {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] loadTranscodedVideo(String outputKey) {
                throw new UnsupportedOperationException();
            }
        };

        SkateTricksService service = new SkateTricksService(
                fallbackAnalyzer(), repositoryStub(), noOpPublisher(), transcoder, null, null, observability());

        byte[] result = service.convertVideo(input, "clip.mov");

        assertArrayEquals(converted, result);
        assertArrayEquals(input, inputRef.get());
        assertEquals("clip.mov", filenameRef.get());
    }

    @Test
    void analyzeConvertedVideoUsesVideoAnalyzerPath() {
        byte[] video = {5, 4, 3};
        AtomicReference<byte[]> analyzedVideo = new AtomicReference<>();
        TrickAnalysisResult analysis = new TrickAnalysisResult(
                SupportedTrick.OLLIE, 91, 87, List.of("Solid pop"), List.of(), "pose-data", "embedding-text");

        TrickAnalyzer analyzer = new TrickAnalyzer() {
            @Override
            public TrickAnalysisResult analyze(List<String> base64Frames) {
                throw new AssertionError("Frame-based analysis should not be used");
            }

            @Override
            public TrickAnalysisResult analyzeVideo(byte[] mp4VideoData) {
                analyzedVideo.set(mp4VideoData);
                return analysis;
            }
        };

        SkateTricksService service = new SkateTricksService(
                analyzer, repositoryStub(), noOpPublisher(), passthroughTranscoder(), null, null, observability());

        TrickAnalysisResult result = service.analyzeConvertedVideo("session-1", video);

        assertEquals(SupportedTrick.OLLIE, result.trick());
        assertEquals(42L, result.attemptId());
        assertArrayEquals(video, analyzedVideo.get());
    }

    @Test
    void analyzeConvertedVideoByOutputKeyLoadsBytesBeforeAnalysis() {
        byte[] video = {7, 8, 9};
        AtomicReference<String> loadedOutputKey = new AtomicReference<>();
        AtomicReference<byte[]> analyzedVideo = new AtomicReference<>();
        TrickAnalysisResult analysis = new TrickAnalysisResult(
                SupportedTrick.KICKFLIP, 88, 84, List.of("Good flick"), List.of(), "pose-data", "embedding-text");

        TrickAnalyzer analyzer = new TrickAnalyzer() {
            @Override
            public TrickAnalysisResult analyze(List<String> base64Frames) {
                throw new AssertionError("Frame-based analysis should not be used");
            }

            @Override
            public TrickAnalysisResult analyzeVideo(byte[] mp4VideoData) {
                analyzedVideo.set(mp4VideoData);
                return analysis;
            }
        };

        VideoTranscoder transcoder = new VideoTranscoder() {
            @Override
            public TranscodedVideo convertToMp4(byte[] videoData, String originalFilename) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TranscodedVideo convertUploadedObjectToMp4(String inputKey, String originalFilename) {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] loadTranscodedVideo(String outputKey) {
                loadedOutputKey.set(outputKey);
                return video;
            }
        };

        SkateTricksService service = new SkateTricksService(
                analyzer, repositoryStub(), noOpPublisher(), transcoder, null, null, observability());

        TrickAnalysisResult result = service.analyzeConvertedVideo("session-2", "skatetricks/output/test/video.mp4");

        assertEquals(SupportedTrick.KICKFLIP, result.trick());
        assertEquals(42L, result.attemptId());
        assertEquals("skatetricks/output/test/video.mp4", loadedOutputKey.get());
        assertArrayEquals(video, analyzedVideo.get());
    }

    private static TrickAnalyzer fallbackAnalyzer() {
        return new TrickAnalyzer() {
            @Override
            public TrickAnalysisResult analyze(List<String> base64Frames) {
                return new TrickAnalysisResult(SupportedTrick.UNKNOWN, 0, 0, List.of());
            }

            @Override
            public TrickAnalysisResult analyzeVideo(byte[] mp4VideoData) {
                return new TrickAnalysisResult(SupportedTrick.UNKNOWN, 0, 0, List.of());
            }
        };
    }

    private static VideoTranscoder passthroughTranscoder() {
        return new VideoTranscoder() {
            @Override
            public VideoTranscoder.TranscodedVideo convertToMp4(byte[] videoData, String originalFilename) {
                return new VideoTranscoder.TranscodedVideo(
                        videoData, "https://cdn.example.com/video.mp4", "skatetricks/output/test/video.mp4");
            }

            @Override
            public VideoTranscoder.TranscodedVideo convertUploadedObjectToMp4(
                    String inputKey, String originalFilename) {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] loadTranscodedVideo(String outputKey) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static ApplicationEventPublisher noOpPublisher() {
        return new ApplicationEventPublisher() {
            @Override
            public void publishEvent(ApplicationEvent event) {}

            @Override
            public void publishEvent(Object event) {}
        };
    }

    private static SkatetricksObservability observability() {
        return new SkatetricksObservability(new SimpleMeterRegistry());
    }

    private static TrickAttemptRepository repositoryStub() {
        return (TrickAttemptRepository) Proxy.newProxyInstance(
                TrickAttemptRepository.class.getClassLoader(),
                new Class<?>[] {TrickAttemptRepository.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "save" -> {
                            TrickAttemptEntity entity = (TrickAttemptEntity) args[0];
                            entity.setId(42L);
                            yield entity;
                        }
                        case "findBySessionIdOrderByCreatedAtDesc" -> List.of();
                        case "findById" -> java.util.Optional.empty();
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "TrickAttemptRepositoryStub";
                        default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                    };
                });
    }
}
