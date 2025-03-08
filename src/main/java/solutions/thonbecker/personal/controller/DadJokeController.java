package solutions.thonbecker.personal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import solutions.thonbecker.personal.types.JokeRequest;
import solutions.thonbecker.personal.types.JokeResponse;

@RestController
@RequestMapping("/api/joke")
@Slf4j
@RequiredArgsConstructor
public class DadJokeController {
    private final ObjectMapper objectMapper;
    private static final String JOKE_API_URL = "https://ondxpdql18.execute-api.us-east-1.amazonaws.com/joke";
    private static final String DAD_JOKE_API_URL = "https://icanhazdadjoke.com/";
    private static final String CDN_DOMAIN_NAME = "https://cdn.thonbecker.com";

    @GetMapping
    public ResponseEntity<String> getJoke() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();

        // First, get the joke text
        HttpHeaders jokeHeaders = new HttpHeaders();
        jokeHeaders.set("Accept", "text/plain");
        HttpEntity<String> jokeEntity = new HttpEntity<>(jokeHeaders);

        ResponseEntity<String> jokeResponse =
                restTemplate.exchange(DAD_JOKE_API_URL, HttpMethod.GET, jokeEntity, String.class);

        if (!jokeResponse.getStatusCode().is2xxSuccessful() || jokeResponse.getBody() == null) {
            return ResponseEntity.notFound().build();
        }

        String url = UriComponentsBuilder.fromUriString(JOKE_API_URL)
                .queryParam("voice", "Matthew")
                .queryParam("translateFrom", "en")
                .queryParam("translateTo", "en")
                .toUriString();

        // Create the request entity with the joke text as the raw body
        String jokeText = jokeResponse.getBody();

        // Create headers with JSON content type
        HttpHeaders voiceHeaders = new HttpHeaders();
        voiceHeaders.setContentType(MediaType.APPLICATION_JSON);

        // Create the request object
        JokeRequest jokeRequest = new JokeRequest(jokeText);

        // Create HttpEntity with the correct generic type
        HttpEntity<JokeRequest> voiceRequestEntity = new HttpEntity<>(jokeRequest, voiceHeaders);

        try {
            ResponseEntity<JokeResponse> voiceResponse =
                    restTemplate.exchange(url, HttpMethod.POST, voiceRequestEntity, JokeResponse.class);

            if (voiceResponse.getBody() != null && voiceResponse.getBody().Location() != null) {
                String location = voiceResponse.getBody().Location().replaceFirst("https://[^/]+", CDN_DOMAIN_NAME);
                return ResponseEntity.ok(location);
            }
        } catch (Exception e) {
            log.error("Error calling voice API: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing joke: " + e.getMessage());
        }

        return ResponseEntity.notFound().build();
    }
}
