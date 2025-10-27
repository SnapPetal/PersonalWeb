package biz.thonbecker.personal.content.infrastructure.web;

import biz.thonbecker.personal.content.domain.BibleVerse;
import biz.thonbecker.personal.content.domain.BibleVerseResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/bible")
@Slf4j
public class BibleVerseController {
    private static final String BIBLE_VERSION = "KJV";
    private static final String VERSE_FORMAT = "%s (%s %s:%s)";
    private static final String FETCH_ERROR_MESSAGE = "Failed to fetch bible verse";

    private final RestTemplate restTemplate;
    private final Cache<LocalDate, BibleVerse> verseCache;
    private final String bibleVerseApiUrl;
    private final int cacheDurationHours;

    public BibleVerseController(
            RestTemplate restTemplate,
            @Value("${bible.verse.api.url}") String bibleVerseApiUrl,
            @Value("${bible.verse.cache.duration:24}") int cacheDurationHours) {
        this.restTemplate = restTemplate;
        this.bibleVerseApiUrl = bibleVerseApiUrl;
        this.cacheDurationHours = cacheDurationHours;
        this.verseCache = createVerseCache();
        configureRestTemplate();
    }

    @GetMapping(value = "/verse-of-day", produces = MediaType.APPLICATION_JSON_VALUE)
    @Retryable(backoff = @Backoff(delay = 1000))
    public ResponseEntity<BibleVerse> getVerseOfTheDay() {
        LocalDate today = LocalDate.now();
        BibleVerse verse = verseCache.get(today, key -> fetchDailyBibleVerse());
        return ResponseEntity.ok(verse);
    }

    private Cache<LocalDate, BibleVerse> createVerseCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(cacheDurationHours, TimeUnit.HOURS)
                .build();
    }

    private void configureRestTemplate() {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setAccept(List.of(MediaType.APPLICATION_JSON));
            return execution.execute(request, body);
        });
    }

    private BibleVerse fetchDailyBibleVerse() {
        try {
            // First try to get the response as a string since the API returns text/plain
            ResponseEntity<String> response =
                    restTemplate.getForEntity(bibleVerseApiUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseBody = response.getBody().trim();

                // Check if the response looks like JSON
                if (responseBody.startsWith("{") && responseBody.endsWith("}")) {
                    // Parse the JSON manually
                    ObjectMapper objectMapper = new ObjectMapper();
                    BibleVerseResponse bibleVerseResponse =
                            objectMapper.readValue(responseBody, BibleVerseResponse.class);
                    return formatBibleVerse(bibleVerseResponse);
                } else {
                    // Not JSON, throw an error
                    throw new BibleVerseFetchException("API returned non-JSON response");
                }
            }
            throw new BibleVerseFetchException(FETCH_ERROR_MESSAGE);
        } catch (Exception e) {
            log.error("Error fetching bible verse: ", e);
            throw new BibleVerseFetchException(FETCH_ERROR_MESSAGE, e);
        }
    }

    private BibleVerse formatBibleVerse(BibleVerseResponse response) {
        String text = response.text().get(BIBLE_VERSION);
        String formattedText = String.format(
                VERSE_FORMAT, text, response.book(), response.chapter(), response.verse());
        return new BibleVerse(formattedText, BIBLE_VERSION);
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
