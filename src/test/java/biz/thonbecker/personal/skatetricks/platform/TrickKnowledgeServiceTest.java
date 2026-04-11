package biz.thonbecker.personal.skatetricks.platform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class TrickKnowledgeServiceTest {

    @Test
    void loadsYamlCatalogAndBuildsPromptSection() {
        final var service =
                new TrickKnowledgeService(new DefaultResourceLoader(), true, "classpath:skatetricks/trick-catalog.yml");

        final var promptSection = service.buildPromptSection();

        assertFalse(promptSection.isBlank());
        assertTrue(promptSection.contains("CURATED TRICK KNOWLEDGE"));
        assertTrue(promptSection.contains("KICKFLIP"));
        assertTrue(promptSection.contains("common confusions"));
        assertTrue(promptSection.contains("HEELFLIP"));
    }
}
