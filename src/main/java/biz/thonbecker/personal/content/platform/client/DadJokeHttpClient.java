package biz.thonbecker.personal.content.platform.client;

import biz.thonbecker.personal.content.domain.DadJokeApiResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

/**
 * HTTP Interface for the Dad Joke API (icanhazdadjoke.com).
 * This interface uses Spring's declarative HTTP client.
 */
public interface DadJokeHttpClient {

    @GetExchange("/")
    DadJokeApiResponse getRandomJoke(@RequestHeader("Accept") String accept);
}
