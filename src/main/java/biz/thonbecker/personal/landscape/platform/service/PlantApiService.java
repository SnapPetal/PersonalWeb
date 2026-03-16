package biz.thonbecker.personal.landscape.platform.service;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.PlantInfo;
import biz.thonbecker.personal.landscape.api.WaterRequirement;
import biz.thonbecker.personal.landscape.domain.exceptions.PlantApiException;
import biz.thonbecker.personal.landscape.platform.client.PerenualPlantHttpClient;
import biz.thonbecker.personal.landscape.platform.client.model.PerenualPlant;
import biz.thonbecker.personal.landscape.platform.client.model.PerenualPlantDetail;
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
 * Service for fetching plant data from the Perenual Plant API.
 *
 * <p>Provides caching and retry logic for resilient API access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlantApiService {

    private final PerenualPlantHttpClient httpClient;

    /**
     * Retrieves detailed information for a specific plant by name/symbol.
     *
     * <p>Searches the Perenual API by the given identifier and returns the first match.
     * Results are cached for 24 hours.
     *
     * @param usdaSymbol Plant name or identifier to search for
     * @return Detailed plant information
     */
    @Cacheable(value = "plantData", key = "#usdaSymbol")
    @Retryable(
            noRetryFor =
                    org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests.class,
            backoff = @Backoff(delay = 1000))
    public PlantInfo getPlantDetails(final String usdaSymbol) {
        try {
            log.debug("Fetching plant details from Perenual API: {}", usdaSymbol);
            final var response = httpClient.searchPlants(usdaSymbol, 1);

            if (Objects.isNull(response.data()) || response.data().isEmpty()) {
                throw new PlantApiException("No plant found for: " + usdaSymbol);
            }

            final var firstResult = response.data().getFirst();

            // Fetch full details for richer data (sunlight, watering, hardiness)
            try {
                final var detail = httpClient.getPlantDetails(firstResult.id());
                return convertDetailToPlantInfo(detail);
            } catch (final Exception e) {
                log.warn(
                        "Failed to fetch detail for plant id {}, using search data: {}",
                        firstResult.id(),
                        e.getMessage());
                return convertSearchResultToPlantInfo(firstResult, null);
            }
        } catch (final PlantApiException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Failed to fetch plant details for {}: {}", usdaSymbol, e.getMessage(), e);
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
    @Retryable(
            noRetryFor =
                    org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests.class,
            backoff = @Backoff(delay = 1000))
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

            final var response = httpClient.searchPlants(query, 1);

            final var plants = Objects.nonNull(response.data())
                    ? response.data().stream()
                            .map(data -> convertSearchResultToPlantInfo(data, zone))
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
     * Fetches plants suitable for the given hardiness zone from Perenual.
     *
     * <p>Results are cached for 24 hours by zone. Filters out plants without real images.
     *
     * @param zone Hardiness zone to search for
     * @return List of plants suitable for the zone
     */
    @Cacheable(value = "plantsByZone", key = "#zone")
    @Retryable(
            noRetryFor =
                    org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests.class,
            backoff = @Backoff(delay = 1000))
    public List<PlantInfo> getPlantsByZone(final HardinessZone zone) {
        try {
            log.debug("Fetching plants for zone {}", zone);

            final var response = httpClient.searchByHardinessZone(zone.getZoneNumber(), 1);

            final var plants = Objects.nonNull(response.data())
                    ? response.data().stream()
                            .filter(p -> hasRealImage(p))
                            .map(data -> convertSearchResultToPlantInfo(data, zone))
                            .filter(Objects::nonNull)
                            .toList()
                    : List.<PlantInfo>of();

            log.info("Found {} plants for zone {}", plants.size(), zone);
            return plants;

        } catch (final org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            final var retryAfter = e.getHeaders().getFirst("Retry-After");
            log.warn(
                    "Perenual API rate limit reached for zone {} (Retry-After: {})",
                    zone,
                    Objects.nonNull(retryAfter) ? retryAfter : "not specified");
            throw new PlantApiException(
                    "Plant API rate limit reached. Please try again later."
                            + (Objects.nonNull(retryAfter) ? " Retry after: " + retryAfter + " seconds." : ""),
                    e);
        } catch (final Exception e) {
            log.error("Failed to fetch plants for zone {}: {}", zone, e.getMessage(), e);
            throw new PlantApiException("Failed to fetch plants for zone " + zone, e);
        }
    }

    /**
     * Checks if a Perenual plant has a real image (not the upgrade_access placeholder).
     */
    private boolean hasRealImage(final PerenualPlant plant) {
        if (Objects.isNull(plant.defaultImage())) {
            return false;
        }
        final var thumbnail = plant.defaultImage().thumbnail();
        return Objects.nonNull(thumbnail) && !thumbnail.isBlank() && !thumbnail.contains("upgrade_access");
    }

    /**
     * Converts a Perenual search result to domain PlantInfo.
     */
    private PlantInfo convertSearchResultToPlantInfo(final PerenualPlant data, final HardinessZone zone) {
        final var scientificName =
                Objects.nonNull(data.scientificName()) && !data.scientificName().isEmpty()
                        ? data.scientificName().getFirst()
                        : null;

        return new PlantInfo(
                String.valueOf(data.id()),
                scientificName,
                data.commonName(),
                data.family(),
                Objects.nonNull(zone) ? List.of(zone) : List.of(),
                LightRequirement.FULL_SUN,
                WaterRequirement.MEDIUM,
                null,
                null,
                null,
                null,
                extractImageUrl(data));
    }

    private String extractImageUrl(final PerenualPlant plant) {
        return extractDefaultImageUrl(plant.defaultImage());
    }

    private String extractDetailImageUrl(final PerenualPlantDetail detail) {
        return extractDefaultImageUrl(detail.defaultImage());
    }

    /**
     * Extracts the best available image URL from a Perenual DefaultImage,
     * filtering out the "upgrade_access" placeholder.
     */
    private String extractDefaultImageUrl(final PerenualPlant.DefaultImage image) {
        if (Objects.isNull(image)) {
            return null;
        }

        for (final var url : new String[] {
            image.thumbnail(), image.smallUrl(), image.mediumUrl(), image.regularUrl(), image.originalUrl()
        }) {
            if (Objects.nonNull(url) && !url.isBlank() && !url.contains("upgrade_access")) {
                return url;
            }
        }

        return null;
    }

    /**
     * Converts a Perenual detail response to domain PlantInfo with richer data.
     */
    private PlantInfo convertDetailToPlantInfo(final PerenualPlantDetail detail) {
        final var scientificName = Objects.nonNull(detail.scientificName())
                        && !detail.scientificName().isEmpty()
                ? detail.scientificName().getFirst()
                : null;

        final var light = mapSunlightToLightRequirement(detail.sunlight());
        final var water = mapWateringToWaterRequirement(detail.watering());
        final var zones = mapHardinessToZones(detail.hardiness());

        return new PlantInfo(
                String.valueOf(detail.id()),
                scientificName,
                detail.commonName(),
                detail.family(),
                zones,
                light,
                water,
                detail.type(),
                null,
                null,
                null,
                extractDetailImageUrl(detail));
    }

    /**
     * Maps Perenual sunlight values to LightRequirement.
     */
    private LightRequirement mapSunlightToLightRequirement(final List<String> sunlight) {
        if (Objects.isNull(sunlight) || sunlight.isEmpty()) {
            return LightRequirement.FULL_SUN;
        }

        final var primary = sunlight.getFirst().toLowerCase();
        if (primary.contains("full shade") || primary.contains("deep shade")) {
            return LightRequirement.FULL_SHADE;
        } else if (primary.contains("part shade") || primary.contains("partial") || primary.contains("filtered")) {
            return LightRequirement.PARTIAL_SHADE;
        }
        return LightRequirement.FULL_SUN;
    }

    /**
     * Maps Perenual watering value to WaterRequirement.
     */
    private WaterRequirement mapWateringToWaterRequirement(final String watering) {
        if (Objects.isNull(watering)) {
            return WaterRequirement.MEDIUM;
        }

        return switch (watering.toLowerCase()) {
            case "minimum", "none" -> WaterRequirement.LOW;
            case "frequent", "abundant" -> WaterRequirement.HIGH;
            default -> WaterRequirement.MEDIUM;
        };
    }

    /**
     * Maps Perenual hardiness min/max to a list of HardinessZone values.
     */
    private List<HardinessZone> mapHardinessToZones(final PerenualPlantDetail.Hardiness hardiness) {
        if (Objects.isNull(hardiness) || Objects.isNull(hardiness.min()) || Objects.isNull(hardiness.max())) {
            return List.of();
        }

        try {
            final var min = parseZoneNumber(hardiness.min());
            final var max = parseZoneNumber(hardiness.max());

            if (min < 1 || max < 1 || min > 13 || max > 13) {
                return List.of();
            }

            final var zones = new ArrayList<HardinessZone>();
            for (var i = min; i <= max; i++) {
                zones.add(HardinessZone.values()[i - 1]);
            }
            return List.copyOf(zones);
        } catch (final NumberFormatException e) {
            log.debug("Failed to parse hardiness zones: min={}, max={}", hardiness.min(), hardiness.max());
            return List.of();
        }
    }

    /**
     * Parses a zone number from a string like "5", "5a", or "5b".
     */
    private int parseZoneNumber(final String zone) {
        final var trimmed = zone.trim().replaceAll("[^0-9]", "");
        return Integer.parseInt(trimmed);
    }
}
