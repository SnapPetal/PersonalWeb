package biz.thonbecker.personal.landscape.platform.configuration;

import biz.thonbecker.personal.landscape.platform.client.UsdaPlantHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration for USDA Plants API HTTP client.
 */
@Configuration
public class LandscapeHttpClientConfig {

    @Bean
    UsdaPlantHttpClient usdaPlantHttpClient(@Value("${landscape.usda-api.base-url}") final String baseUrl) {

        final var webClient = WebClient.builder().baseUrl(baseUrl).build();

        final var factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                .build();

        return factory.createClient(UsdaPlantHttpClient.class);
    }
}
