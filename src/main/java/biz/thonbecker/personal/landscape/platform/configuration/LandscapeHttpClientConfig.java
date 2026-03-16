package biz.thonbecker.personal.landscape.platform.configuration;

import biz.thonbecker.personal.landscape.platform.client.UsdaPlantHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for USDA Plants Services API HTTP client.
 */
@Configuration
public class LandscapeHttpClientConfig {

    @Bean
    UsdaPlantHttpClient usdaPlantHttpClient(@Value("${landscape.usda-api.base-url}") final String baseUrl) {
        final var webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        return new UsdaPlantHttpClient(webClient);
    }
}
