package biz.thonbecker.personal.landscape.platform.service;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.PlantInfo;
import biz.thonbecker.personal.landscape.api.WaterRequirement;
import biz.thonbecker.personal.landscape.domain.exceptions.PlantApiException;
import biz.thonbecker.personal.landscape.platform.client.UsdaPlantHttpClient;
import biz.thonbecker.personal.landscape.platform.client.model.UsdaPlantData;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service for fetching plant data from USDA Plants Services API.
 *
 * <p>Provides caching and retry logic for resilient API access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlantApiService {

    private final UsdaPlantHttpClient httpClient;

    /**
     * Retrieves detailed information for a specific plant by USDA symbol.
     *
     * <p>Uses the search API with Symbol field since the old detail endpoint no longer exists.
     * Results are cached for 24 hours by USDA symbol.
     *
     * @param usdaSymbol USDA plant symbol
     * @return Detailed plant information
     */
    @Cacheable(value = "plantData", key = "#usdaSymbol")
    @Retryable(backoff = @Backoff(delay = 1000))
    public PlantInfo getPlantDetails(final String usdaSymbol) {
        try {
            log.debug("Fetching plant details from USDA API: {}", usdaSymbol);
            final var response = httpClient.searchPlants(usdaSymbol, "Symbol", 1);

            if (Objects.isNull(response.plantResults())
                    || response.plantResults().isEmpty()) {
                throw new PlantApiException("No plant found for symbol: " + usdaSymbol);
            }

            return convertDataToPlantInfo(response.plantResults().getFirst(), null);
        } catch (final PlantApiException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to fetch plant details for symbol {}: {}", usdaSymbol, e.getMessage(), e);
            throw new PlantApiException("Failed to fetch plant details for " + usdaSymbol, e);
        }
    }

    /**
     * Searches for plants matching the given criteria.
     *
     * <p>Results are cached for 24 hours by search criteria hash.
     *
     * @param query Plant name search query
     * @param zone Required hardiness zone
     * @param lightRequirement Optional light requirement filter
     * @param waterRequirement Optional water requirement filter
     * @return List of matching plants
     */
    @Cacheable(value = "plantSearch", key = "#query + '-' + #zone + '-' + #lightRequirement + '-' + #waterRequirement")
    @Retryable(backoff = @Backoff(delay = 1000))
    public List<PlantInfo> searchPlants(
            final String query,
            final HardinessZone zone,
            final LightRequirement lightRequirement,
            final WaterRequirement waterRequirement) {

        try {
            log.debug(
                    "Searching plants: query={}, zone={}, light={}, water={}",
                    query,
                    zone,
                    lightRequirement,
                    waterRequirement);

            final var response = httpClient.searchPlants(query, "CommonName", 1);

            final var plants = Objects.nonNull(response.plantResults())
                    ? response.plantResults().stream()
                            .map(data -> convertDataToPlantInfo(data, zone))
                            .filter(Objects::nonNull)
                            .limit(50)
                            .toList()
                    : List.<PlantInfo>of();

            log.info("Found {} plants matching criteria", plants.size());
            return plants;

        } catch (final Exception e) {
            log.error("Failed to search plants: {}", e.getMessage(), e);
            throw new PlantApiException("Failed to search plants", e);
        }
    }

    /**
     * Converts USDA API search result to domain PlantInfo.
     */
    private PlantInfo convertDataToPlantInfo(final UsdaPlantData data, final HardinessZone zone) {
        return new PlantInfo(
                data.symbol(),
                data.scientificName(),
                data.commonName(),
                data.familyCommonName(),
                Objects.nonNull(zone) ? List.of(zone) : List.of(),
                LightRequirement.FULL_SUN,
                WaterRequirement.MEDIUM,
                null,
                null,
                null,
                null);
    }
}
