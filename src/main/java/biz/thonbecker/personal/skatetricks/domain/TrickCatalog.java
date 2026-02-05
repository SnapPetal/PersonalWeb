package biz.thonbecker.personal.skatetricks.domain;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TrickCatalog {

    private TrickCatalog() {}

    public static String buildTrickDescriptions() {
        return Stream.of(SupportedTrick.values())
                .filter(t -> t != SupportedTrick.UNKNOWN)
                .map(t -> "- " + t.getDisplayName() + ": " + t.getDescription())
                .collect(Collectors.joining("\n"));
    }

    public static SupportedTrick fromName(String name) {
        if (name == null || name.isBlank()) {
            return SupportedTrick.UNKNOWN;
        }
        String normalized = name.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        try {
            return SupportedTrick.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            for (SupportedTrick trick : SupportedTrick.values()) {
                if (trick.getDisplayName().equalsIgnoreCase(name.trim())) {
                    return trick;
                }
            }
            return SupportedTrick.UNKNOWN;
        }
    }
}
