package biz.thonbecker.personal.skatetricks.platform;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SkatetricksObservability {

    private final MeterRegistry meterRegistry;

    SkatetricksObservability(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Scope start(String operation) {
        return new Scope(operation, System.nanoTime());
    }

    public void incrementStage(String stage, String outcome, String... extraTags) {
        meterRegistry
                .counter(
                        "skatetricks.stage.events",
                        tags(append(new String[] {"stage", stage, "outcome", outcome}, extraTags)))
                .increment();
    }

    public void recordPayloadSize(String name, long bytes, String... extraTags) {
        meterRegistry
                .summary("skatetricks.payload.bytes", tags(append(new String[] {"name", name}, extraTags)))
                .record(bytes);
    }

    public void recordFrameCount(int frameCount, String... extraTags) {
        meterRegistry.summary("skatetricks.analysis.frames", tags(extraTags)).record(frameCount);
    }

    public void success(Scope scope, String... extraTags) {
        record(scope, "success", extraTags);
    }

    public void failure(Scope scope, Throwable throwable, String... extraTags) {
        final var errorType = Objects.nonNull(throwable) ? throwable.getClass().getSimpleName() : "Unknown";
        record(scope, "failure", append(extraTags, "error", errorType));
    }

    private void record(Scope scope, String outcome, String... extraTags) {
        final var tags = tags(append(new String[] {"operation", scope.operation(), "outcome", outcome}, extraTags));
        meterRegistry.counter("skatetricks.operation.events", tags).increment();
        Timer.builder("skatetricks.operation.duration")
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofNanos(System.nanoTime() - scope.startedAtNanos()));
    }

    private static String[] tags(String... values) {
        final var tags = new ArrayList<String>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            if (Objects.isNull(values[i + 1]) || values[i + 1].isBlank()) {
                continue;
            }
            tags.add(values[i]);
            tags.add(values[i + 1]);
        }
        return tags.toArray(String[]::new);
    }

    private static String[] append(String[] existing, String... additional) {
        final var values = new ArrayList<String>();
        for (String value : existing) {
            values.add(value);
        }
        for (String value : additional) {
            values.add(value);
        }
        return values.toArray(String[]::new);
    }

    public record Scope(String operation, long startedAtNanos) {}
}
