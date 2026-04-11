package biz.thonbecker.personal.skatetricks.platform;

import biz.thonbecker.personal.skatetricks.api.SupportedTrick;
import java.util.List;

public record SkatetricksKnowledgeCatalog(List<TrickKnowledgeEntry> tricks) {

    public record TrickKnowledgeEntry(
            SupportedTrick trick,
            String category,
            List<String> aliases,
            List<String> keyCues,
            List<String> disqualifiers,
            List<SupportedTrick> commonConfusions) {}
}
