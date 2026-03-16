package biz.thonbecker.personal.landscape.platform.configuration;

import biz.thonbecker.personal.landscape.platform.client.PerenualPlantHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Perenual Plant API HTTP client.
 */
@Configuration
public class LandscapeHttpClientConfig {

    @Bean
    PerenualPlantHttpClient perenualPlantHttpClient(
            @Value("${landscape.plant-api.base-url}") final String baseUrl,
            @Value("${landscape.plant-api.key}") final String apiKey) {
        final var webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        return new PerenualPlantHttpClient(webClient, apiKey);
    }
}
