package biz.thonbecker.personal.content.platform.client;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

/**
 * HTTP Interface for the Bible Verse API.
 * This interface uses Spring's declarative HTTP client.
 */
public interface BibleVerseHttpClient {

    @GetExchange
    String getDailyVerse(@RequestHeader("Accept") String accept);
}
