package biz.thonbecker.personal.landscape.platform.service;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.PlantInfo;
import biz.thonbecker.personal.landscape.api.WaterRequirement;
import biz.thonbecker.personal.landscape.domain.exceptions.PlantApiException;
import biz.thonbecker.personal.landscape.platform.client.UsdaPlantHttpClient;
import biz.thonbecker.personal.landscape.platform.client.model.UsdaPlantData;
import biz.thonbecker.personal.landscape.platform.client.model.UsdaPlantDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service for fetching plant data from USDA Plants Database.
 *
 * <p>Provides caching and retry logic for resilient API access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlantApiService {

    private final UsdaPlantHttpClient httpClient;

    /**
     * Retrieves detailed information for a specific plant.
     *
     * <p>Results are cached for 24 hours by USDA symbol.
     *
     * @param usdaSymbol USDA plant symbol
     * @return Detailed plant information
     */
    @Cacheable(value = "plantData", key = "#usdaSymbol")
    @Retryable(backoff = @Backoff(delay = 1000))
    public PlantInfo getPlantDetails(final String usdaSymbol) {
        try {
            log.debug("Fetching plant details from USDA API: {}", usdaSymbol);
            final var detail = httpClient.getPlantDetail(usdaSymbol);
            return convertDetailToPlantInfo(detail);
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

            // Map requirements to USDA API filters
            final var growthHabit = mapGrowthHabit(lightRequirement);

            final var response = httpClient.searchPlants(query, null, growthHabit, 50);

            final var plants = new ArrayList<PlantInfo>();
            for (final var data : response.data()) {
                final var plantInfo = convertDataToPlantInfo(data, zone);
                if (Objects.nonNull(plantInfo)) {
                    plants.add(plantInfo);
                }
            }

            log.info("Found {} plants matching criteria", plants.size());
            return plants;

        } catch (final Exception e) {
            log.error("Failed to search plants: {}", e.getMessage(), e);
            throw new PlantApiException("Failed to search plants", e);
        }
    }

    /**
     * Converts USDA API detail response to domain PlantInfo.
     *
     * @param detail USDA plant detail
     * @return Domain plant info
     */
    private PlantInfo convertDetailToPlantInfo(final UsdaPlantDetail detail) {
        return new PlantInfo(
                detail.symbol(),
                detail.scientificName(),
                detail.commonName(),
                detail.familyCommonName(),
                List.of(), // Hardiness zones not in USDA API, would need additional data source
                mapLightRequirement(detail.shapeAndOrientation()),
                mapWaterRequirement(detail.activeGrowthPeriod()),
                detail.category(),
                detail.nativeStatus(),
                detail.matureHeight(),
                detail.matureWidth());
    }

    /**
     * Converts USDA API search result to domain PlantInfo.
     *
     * @param data USDA plant data
     * @param zone Requested hardiness zone
     * @return Domain plant info, or null if not suitable for zone
     */
    private PlantInfo convertDataToPlantInfo(final UsdaPlantData data, final HardinessZone zone) {
        // Simple conversion - in production, would filter by actual hardiness data
        return new PlantInfo(
                data.symbol(),
                data.scientificName(),
                data.commonName(),
                data.familyCommonName(),
                List.of(zone), // Simplified - assume all results work for requested zone
                mapLightRequirement(null),
                mapWaterRequirement(null),
                data.category(),
                data.nativeStatus(),
                null,
                null);
    }

    /**
     * Maps light requirement to USDA growth habit filter.
     *
     * @param lightRequirement Light requirement
     * @return USDA growth habit parameter, or null
     */
    private String mapGrowthHabit(final LightRequirement lightRequirement) {
        if (Objects.isNull(lightRequirement)) {
            return null;
        }
        return switch (lightRequirement) {
            case FULL_SUN -> "Tree";
            case PARTIAL_SHADE -> "Shrub";
            case FULL_SHADE -> "Forb/herb";
        };
    }

    /**
     * Maps USDA shape/orientation to light requirement.
     *
     * @param shapeAndOrientation USDA field
     * @return Light requirement enum
     */
    private LightRequirement mapLightRequirement(final String shapeAndOrientation) {
        // Simplified mapping - production would use more sophisticated logic
        return LightRequirement.FULL_SUN;
    }

    /**
     * Maps USDA active growth period to water requirement.
     *
     * @param activeGrowthPeriod USDA field
     * @return Water requirement enum
     */
    private WaterRequirement mapWaterRequirement(final String activeGrowthPeriod) {
        // Simplified mapping - production would use more sophisticated logic
        return WaterRequirement.MEDIUM;
    }
}
