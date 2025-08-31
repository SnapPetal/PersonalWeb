package solutions.thonbecker.personal.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import solutions.thonbecker.personal.types.JokeResponse;

@RestController
@RequestMapping("/api/joke")
@Slf4j
@RequiredArgsConstructor
public class DadJokeController {
    private static final String JOKE_API_URL = "https://ondxpdql18.execute-api.us-east-1.amazonaws.com/joke";
    private static final String DAD_JOKE_API_URL = "https://icanhazdadjoke.com/";
    private static final String CDN_DOMAIN_NAME = "https://cdn.thonbecker.com";
    private final RestTemplate restTemplate;

    @GetMapping
    public ResponseEntity<String> getJoke() {
        String jokeText = fetchDadJoke();
        if (jokeText == null) {
            return ResponseEntity.notFound().build();
        }

        String audioLocation = convertJokeToAudio(jokeText);
        if (audioLocation != null) {
            return ResponseEntity.ok(audioLocation);
        }

        return ResponseEntity.notFound().build();
    }

    private String fetchDadJoke() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "text/plain");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(DAD_JOKE_API_URL, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error fetching dad joke: {}", e.getMessage());
        }

        return null;
    }

    private String convertJokeToAudio(String jokeText) {
        String url = UriComponentsBuilder.fromUriString(JOKE_API_URL)
                .queryParam("voice", "Matthew")
                .queryParam("translateFrom", "en")
                .queryParam("translateTo", "en")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> requestEntity = new HttpEntity<>(jokeText, headers);

        try {
            ResponseEntity<JokeResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, JokeResponse.class);

            if (response.getBody() != null && response.getBody().Location() != null) {
                return response.getBody().Location().replaceFirst("https://[^/]+", CDN_DOMAIN_NAME);
            }
        } catch (Exception e) {
            log.error("Error converting joke to audio: {}", e.getMessage());
        }

        return null;
    }
}
