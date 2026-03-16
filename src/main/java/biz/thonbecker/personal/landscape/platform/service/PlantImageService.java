package biz.thonbecker.personal.landscape.platform.service;

import biz.thonbecker.personal.landscape.platform.client.PerenualPlantHttpClient;
import biz.thonbecker.personal.landscape.platform.client.model.PerenualPlant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for fetching plant images from the Perenual Plant API.
 *
 * <p>Searches Perenual by plant name and returns the thumbnail URL from the
 * search response. Results are cached for 24 hours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlantImageService {

    private final PerenualPlantHttpClient perenualClient;

    /**
     * Gets a plant image URL by searching the Perenual API with the given plant name or symbol.
     *
     * @param plantName Plant name or identifier to search for
     * @return Thumbnail image URL if available, or null
     */
    @Cacheable(value = "plantImages", key = "#plantName")
    public String getPlantImageUrl(final String plantName) {
        if (Objects.isNull(plantName) || plantName.isBlank()) {
            return null;
        }

        try {
            final var response = perenualClient.searchPlants(plantName, 1);

            if (Objects.nonNull(response.data())) {
                for (final var plant : response.data()) {
                    final var imageUrl = extractThumbnailUrl(plant);
                    if (Objects.nonNull(imageUrl)) {
                        log.info("Found Perenual image for '{}': {}", plantName, imageUrl);
                        return imageUrl;
                    }
                }
            }
        } catch (final Exception e) {
            log.debug("Failed to look up plant image for '{}': {}", plantName, e.getMessage());
        }

        log.info("No image found for '{}'", plantName);
        return null;
    }

    /**
     * Extracts the best available thumbnail URL from a Perenual plant record.
     */
    private String extractThumbnailUrl(final PerenualPlant plant) {
        final var image = plant.defaultImage();
        if (Objects.isNull(image)) {
            return null;
        }

        if (Objects.nonNull(image.thumbnail()) && !image.thumbnail().isBlank()) {
            return image.thumbnail();
        }
        if (Objects.nonNull(image.smallUrl()) && !image.smallUrl().isBlank()) {
            return image.smallUrl();
        }
        if (Objects.nonNull(image.mediumUrl()) && !image.mediumUrl().isBlank()) {
            return image.mediumUrl();
        }
        if (Objects.nonNull(image.regularUrl()) && !image.regularUrl().isBlank()) {
            return image.regularUrl();
        }
        if (Objects.nonNull(image.originalUrl()) && !image.originalUrl().isBlank()) {
            return image.originalUrl();
        }

        return null;
    }
}
