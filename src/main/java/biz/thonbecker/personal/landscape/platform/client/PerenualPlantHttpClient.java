package biz.thonbecker.personal.landscape.platform.client;

import biz.thonbecker.personal.landscape.platform.client.model.PerenualPlantDetail;
import biz.thonbecker.personal.landscape.platform.client.model.PerenualPlantSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * HTTP client for the Perenual Plant API.
 *
 * <p>Provides plant search and detail lookups using the Perenual v2 API.
 */
@RequiredArgsConstructor
@Slf4j
public class PerenualPlantHttpClient {

    private final WebClient webClient;
    private final String apiKey;

    /**
     * Searches for plants by name.
     *
     * @param query Plant name (scientific or common)
     * @param page Page number (1-based)
     * @return Search response with matching plants
     */
    public PerenualPlantSearchResponse searchPlants(final String query, final int page) {
        log.debug("Searching Perenual API: query={}, page={}", query, page);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/species-list")
                        .queryParam("key", apiKey)
                        .queryParam("q", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(PerenualPlantSearchResponse.class)
                .block();
    }

    /**
     * Searches for plants by hardiness zone.
     *
     * @param hardinessZone USDA hardiness zone number (e.g., 7)
     * @param page Page number (1-based)
     * @return Search response with plants suitable for the zone
     */
    public PerenualPlantSearchResponse searchByHardinessZone(final int hardinessZone, final int page) {
        log.debug("Searching Perenual API by hardiness zone: zone={}, page={}", hardinessZone, page);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/species-list")
                        .queryParam("key", apiKey)
                        .queryParam("hardiness", hardinessZone)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(PerenualPlantSearchResponse.class)
                .block();
    }

    /**
     * Retrieves detailed information for a specific plant by Perenual ID.
     *
     * @param id Perenual plant species ID
     * @return Detailed plant information
     */
    public PerenualPlantDetail getPlantDetails(final int id) {
        log.debug("Fetching Perenual plant details: id={}", id);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/species/details/{id}")
                        .queryParam("key", apiKey)
                        .build(id))
                .retrieve()
                .bodyToMono(PerenualPlantDetail.class)
                .block();
    }
}
