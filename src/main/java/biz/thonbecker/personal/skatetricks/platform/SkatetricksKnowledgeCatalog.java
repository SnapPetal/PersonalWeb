package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record SkatetricksKnowledgeCatalog(List<TrickKnowledgeEntry> tricks) {

    public record TrickKnowledgeEntry(
            String name,
            @Nullable SupportedTrick supportedTrick,
            String category,
            List<String> aliases,
            List<String> keyCues,
            List<String> disqualifiers,
            List<SupportedTrick> commonConfusions) {}
}
