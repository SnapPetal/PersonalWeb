package biz.thonbecker.personal.content.platform.web;

import biz.thonbecker.personal.content.domain.BibleVerse;
import biz.thonbecker.personal.content.domain.BibleVerseResponse;
import biz.thonbecker.personal.content.platform.client.BibleVerseHttpClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/bible")
@Slf4j
public class BibleVerseController {
    private static final String PREFERRED_VERSION = "CSB";
    private static final String GREEK_VERSION = "Greek";
    private static final String FETCH_ERROR_MESSAGE = "Failed to fetch bible verse";

    private final BibleVerseHttpClient bibleVerseHttpClient;
    private final Cache<LocalDate, BibleVerse> verseCache;
    private final int cacheDurationHours;

    public BibleVerseController(
            BibleVerseHttpClient bibleVerseHttpClient,
            @Value("${bible.verse.cache.duration:24}") int cacheDurationHours) {
        this.bibleVerseHttpClient = bibleVerseHttpClient;
        this.cacheDurationHours = cacheDurationHours;
        this.verseCache = createVerseCache();
    }

    @GetMapping(value = "/verse-of-day", produces = MediaType.APPLICATION_JSON_VALUE)
    @Retryable(backoff = @Backoff(delay = 1000))
    public ResponseEntity<BibleVerse> getVerseOfTheDay() {
        LocalDate today = LocalDate.now();
        BibleVerse verse = verseCache.get(today, _ -> fetchDailyBibleVerse());
        return ResponseEntity.ok(verse);
    }

    @GetMapping(value = "/verse-of-day/fragment", produces = MediaType.TEXT_HTML_VALUE)
    public String getVerseFragment() {
        final var verse = getVerseOfTheDay().getBody();
        if (verse == null) {
            return "<p class=\"muted\">The verse is unavailable right now.</p>";
        }
        final var greek = verse.greekText();
        final var toggle = greek == null ? "" : "<button class=\"verse-toggle\" type=\"button\" data-english=\""
                + HtmlUtils.htmlEscape(verse.text()) + "\" data-greek=\"" + HtmlUtils.htmlEscape(greek)
                + "\">Show Greek</button>";
        return "<p class=\"verse-text\">" + HtmlUtils.htmlEscape(verse.text()) + "</p>"
                + "<p class=\"verse-reference\">" + HtmlUtils.htmlEscape(verse.translation()) + "</p>" + toggle;
    }

    private Cache<LocalDate, BibleVerse> createVerseCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(cacheDurationHours, TimeUnit.HOURS)
                .build();
    }

    private BibleVerse fetchDailyBibleVerse() {
        try {
            String responseBody = bibleVerseHttpClient
                    .getDailyVerse(MediaType.APPLICATION_JSON_VALUE)
                    .trim();

            // Check if the response looks like JSON
            if (responseBody.startsWith("{") && responseBody.endsWith("}")) {
                ObjectMapper objectMapper = new ObjectMapper();
                BibleVerseResponse bibleVerseResponse = objectMapper.readValue(responseBody, BibleVerseResponse.class);
                return formatBibleVerse(bibleVerseResponse);
            } else {
                throw new BibleVerseFetchException("API returned non-JSON response");
            }
        } catch (Exception e) {
            log.error("Error fetching bible verse: ", e);
            throw new BibleVerseFetchException(FETCH_ERROR_MESSAGE, e);
        }
    }

    private BibleVerse formatBibleVerse(BibleVerseResponse response) {
        final var textMap = response.text();
        final var version = textMap.containsKey(PREFERRED_VERSION)
                ? PREFERRED_VERSION
                : textMap.keySet().iterator().next();
        final var text = textMap.get(version);
        final var reference = String.format("(%s %s:%s)", response.book(), response.chapter(), response.verse());
        final var formattedText = String.format("%s %s", text, reference);
        final var greekText = textMap.containsKey(GREEK_VERSION)
                ? String.format("%s %s", textMap.get(GREEK_VERSION), reference)
                : null;
        return new BibleVerse(formattedText, version, greekText);
    }

    private static class BibleVerseFetchException extends RuntimeException {
        public BibleVerseFetchException(String message) {
            super(message);
        }

        public BibleVerseFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
