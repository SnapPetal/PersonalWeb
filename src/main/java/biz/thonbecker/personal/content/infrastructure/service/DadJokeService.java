package biz.thonbecker.personal.content.infrastructure.service;

import biz.thonbecker.personal.content.domain.AudioResult;
import biz.thonbecker.personal.content.domain.AudioStorageService;
import biz.thonbecker.personal.content.domain.DadJokeApiResponse;
import biz.thonbecker.personal.content.domain.TextToSpeechService;
import biz.thonbecker.personal.content.domain.Voice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for retrieving dad jokes and converting them to audio.
 * Orchestrates the workflow of fetching a joke, converting to speech, and storing audio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DadJokeService {

    private static final String DAD_JOKE_API_URL = "https://icanhazdadjoke.com/";
    private static final String AUDIO_CONTENT_TYPE = "audio/mp3";

    private final RestTemplate restTemplate;
    private final TextToSpeechService textToSpeechService;
    private final AudioStorageService audioStorageService;

    /**
     * Retrieves a random dad joke and converts it to audio.
     * Results are cached for 24 hours, so all users get the same joke during that period.
     *
     * @param voice The voice to use for text-to-speech
     * @return AudioResult containing the CDN URL of the audio file, or null if failed
     */
    @Cacheable(value = "jokeAudio", key = "'daily-joke-' + #voice")
    public AudioResult getJokeAudio(final Voice voice) {
        log.info("Fetching new dad joke for voice: {} (cache miss)", voice);

        final var jokeText = fetchDadJoke();
        if (jokeText == null) {
            log.warn("Failed to fetch dad joke");
            return null;
        }

        return convertJokeToAudio(jokeText, voice);
    }

    /**
     * Converts a specific joke text to audio without caching.
     * This is called by the cached getJokeAudio method.
     *
     * @param jokeText The joke text to convert
     * @param voice The voice to use for text-to-speech
     * @return AudioResult containing the CDN URL of the audio file, or null if failed
     */
    private AudioResult convertJokeToAudio(final String jokeText, final Voice voice) {
        log.debug("Converting joke to audio: {}", jokeText.substring(0, Math.min(50, jokeText.length())));

        try {
            final var audioStream = textToSpeechService.convertToSpeech(jokeText, voice);
            final var audioResult = audioStorageService.store(audioStream, AUDIO_CONTENT_TYPE);

            log.info("Successfully created joke audio: {}", audioResult.cdnUrl());
            return audioResult;
        } catch (final Exception e) {
            log.error("Failed to convert joke to audio: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches a random dad joke from the external API.
     *
     * @return The joke text, or null if fetch failed
     */
    private String fetchDadJoke() {
        final var headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        final var entity = new HttpEntity<String>(headers);

        try {
            final var response =
                    restTemplate.exchange(DAD_JOKE_API_URL, HttpMethod.GET, entity, DadJokeApiResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().joke();
            }
        } catch (final Exception e) {
            log.error("Error fetching dad joke: {}", e.getMessage(), e);
        }

        return null;
    }
}
