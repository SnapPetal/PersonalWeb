package biz.thonbecker.personal.content.domain;

import java.util.Map;

public record BibleVerseResponse(
        String book, String chapter, String verse, Map<String, String> text) {}
