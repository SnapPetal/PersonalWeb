package biz.thonbecker.personal.landscape.platform.client;

import biz.thonbecker.personal.landscape.platform.client.model.UsdaPlantSearchResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * HTTP client for USDA Plants Services API.
 *
 * <p>The new USDA API at plantsservices.sc.egov.usda.gov uses POST endpoints
 * with PascalCase JSON request/response bodies.
 */
@RequiredArgsConstructor
@Slf4j
public class UsdaPlantHttpClient {

    private final WebClient webClient;

    /**
     * Searches for plants by name using the new USDA Plants Services API.
     *
     * @param query Plant name (scientific or common)
     * @param field Search field: "CommonName", "ScientificName", or "Symbol"
     * @param pageNumber Page number (1-based)
     * @return Search response with matching plants
     */
    public UsdaPlantSearchResponse searchPlants(final String query, final String field, final int pageNumber) {
        final var body =
                Map.of("SearchCriteria", Map.of("Text", query, "Field", field), "PageNumber", pageNumber, "AllData", 0);

        return webClient
                .post()
                .uri("/plants-search-results")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(UsdaPlantSearchResponse.class)
                .block();
    }
}
