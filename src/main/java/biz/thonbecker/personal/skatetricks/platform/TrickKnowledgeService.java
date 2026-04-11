package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import biz.thonbecker.personal.skatetricks.domain.TrickCatalog;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
@Slf4j
class TrickKnowledgeService {

    private final ResourceLoader resourceLoader;
    private final boolean enabled;
    private final String catalogResource;
    private volatile SkatetricksKnowledgeCatalog cachedCatalog;

    TrickKnowledgeService(
            ResourceLoader resourceLoader,
            @Value("${skatetricks.knowledge.enabled:false}") boolean enabled,
            @Value("${skatetricks.knowledge.catalog-resource:classpath:skatetricks/trick-catalog.yml}")
                    String catalogResource) {
        this.resourceLoader = resourceLoader;
        this.enabled = enabled;
        this.catalogResource = catalogResource;
    }

    String buildPromptSection() {
        if (!enabled) {
            return "";
        }

        final var catalog = loadCatalog();
        if (catalog.tricks().isEmpty()) {
            return "";
        }

        final var prompt = new StringBuilder("\nCURATED TRICK KNOWLEDGE (use this as high-priority reference):\n");
        for (var trick : catalog.tricks()) {
            prompt.append("- ")
                    .append(trick.trick().name())
                    .append(" [category: ")
                    .append(trick.category())
                    .append("]\n");

            if (!trick.aliases().isEmpty()) {
                prompt.append("  aliases: ")
                        .append(String.join(", ", trick.aliases()))
                        .append("\n");
            }
            if (!trick.keyCues().isEmpty()) {
                prompt.append("  key cues: ")
                        .append(String.join("; ", trick.keyCues()))
                        .append("\n");
            }
            if (!trick.disqualifiers().isEmpty()) {
                prompt.append("  disqualifiers: ")
                        .append(String.join("; ", trick.disqualifiers()))
                        .append("\n");
            }
            if (!trick.commonConfusions().isEmpty()) {
                prompt.append("  common confusions: ")
                        .append(trick.commonConfusions().stream()
                                .map(Enum::name)
                                .toList())
                        .append("\n");
            }
        }
        return prompt.toString();
    }

    SkatetricksKnowledgeCatalog loadCatalog() {
        if (Objects.nonNull(cachedCatalog)) {
            return cachedCatalog;
        }

        synchronized (this) {
            if (Objects.nonNull(cachedCatalog)) {
                return cachedCatalog;
            }

            final var resource = resourceLoader.getResource(catalogResource);
            if (!resource.exists()) {
                log.warn("event=trick_catalog_missing resource={}", catalogResource);
                cachedCatalog = new SkatetricksKnowledgeCatalog(List.of());
                return cachedCatalog;
            }

            try (InputStream inputStream = resource.getInputStream();
                    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                final var yaml = new Yaml();
                final var parsed = yaml.load(reader);
                cachedCatalog = parseCatalog(parsed);
                log.info(
                        "event=trick_catalog_loaded resource={} trickCount={}",
                        catalogResource,
                        cachedCatalog.tricks().size());
                return cachedCatalog;
            } catch (Exception e) {
                log.error("event=trick_catalog_load_failed resource={}", catalogResource, e);
                cachedCatalog = new SkatetricksKnowledgeCatalog(List.of());
                return cachedCatalog;
            }
        }
    }

    private static SkatetricksKnowledgeCatalog parseCatalog(Object parsed) {
        if (!(parsed instanceof Map<?, ?> root)) {
            return new SkatetricksKnowledgeCatalog(List.of());
        }
        final var tricks = root.get("tricks");
        if (!(tricks instanceof List<?> trickEntries)) {
            return new SkatetricksKnowledgeCatalog(List.of());
        }

        final var entries = new ArrayList<SkatetricksKnowledgeCatalog.TrickKnowledgeEntry>();
        for (Object trickEntry : trickEntries) {
            if (!(trickEntry instanceof Map<?, ?> trickMap)) {
                continue;
            }
            final var trick = TrickCatalog.fromName(stringValue(trickMap.get("trick")));
            if (trick == SupportedTrick.UNKNOWN) {
                continue;
            }

            entries.add(new SkatetricksKnowledgeCatalog.TrickKnowledgeEntry(
                    trick,
                    defaultValue(stringValue(trickMap.get("category")), "unknown"),
                    stringList(trickMap.get("aliases")),
                    stringList(trickMap.get("keyCues")),
                    stringList(trickMap.get("disqualifiers")),
                    supportedTrickList(trickMap.get("commonConfusions"))));
        }
        return new SkatetricksKnowledgeCatalog(List.copyOf(entries));
    }

    private static List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> values)) {
            return List.of();
        }
        final var result = new ArrayList<String>();
        for (Object value : values) {
            final var stringValue = stringValue(value);
            if (!stringValue.isBlank()) {
                result.add(stringValue);
            }
        }
        return List.copyOf(result);
    }

    private static List<SupportedTrick> supportedTrickList(Object raw) {
        if (!(raw instanceof List<?> values)) {
            return List.of();
        }
        final var result = new ArrayList<SupportedTrick>();
        for (Object value : values) {
            final var trick = TrickCatalog.fromName(stringValue(value));
            if (trick != SupportedTrick.UNKNOWN) {
                result.add(trick);
            }
        }
        return List.copyOf(result);
    }

    private static String stringValue(Object value) {
        return Objects.isNull(value) ? "" : value.toString().trim();
    }

    private static String defaultValue(String value, String fallback) {
        return value.isBlank() ? fallback : value.toLowerCase(Locale.ROOT);
    }
}
