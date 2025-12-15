package biz.thonbecker.personal.content.infrastructure.web;

import biz.thonbecker.personal.content.domain.Voice;
import biz.thonbecker.personal.content.infrastructure.service.DadJokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for dad joke endpoints.
 * Retrieves random dad jokes and converts them to audio using AWS Polly.
 */
@RestController
@RequestMapping("/api/joke")
@Slf4j
@RequiredArgsConstructor
public class DadJokeController {

    private final DadJokeService dadJokeService;

    /**
     * Gets a random dad joke as audio.
     *
     * @param voice The voice to use for text-to-speech (default: MATTHEW)
     * @return ResponseEntity with the CDN URL of the audio file
     */
    @GetMapping
    public ResponseEntity<String> getJoke(@RequestParam(defaultValue = "MATTHEW") final Voice voice) {
        log.debug("Fetching dad joke with voice: {}", voice);

        final var audioResult = dadJokeService.getJokeAudio(voice);

        if (audioResult != null) {
            log.info("Successfully generated joke audio: {}", audioResult.cdnUrl());
            return ResponseEntity.ok(audioResult.cdnUrl());
        }

        log.warn("Failed to generate joke audio");
        return ResponseEntity.notFound().build();
    }
}
