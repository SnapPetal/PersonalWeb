package solutions.thonbecker.personal.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FoosballClient {
    private final RestTemplate restTemplate;

    public FoosballClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Example: GET /api/players
    public <T> T getForObject(String path, Class<T> responseType) {
        return restTemplate.getForObject(path, responseType);
    }

    // Example: POST
    public <T, R> R postForObject(String path, T body, Class<R> responseType) {
        return restTemplate.postForObject(path, body, responseType);
    }
}
