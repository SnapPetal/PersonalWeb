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

        // Try the full name first, then simplified versions
        for (final var query : new String[] {plantName, simplifyPlantName(plantName)}) {
            if (query.isBlank()) {
                continue;
            }
            try {
                final var response = perenualClient.searchPlants(query, 1);

                if (Objects.nonNull(response.data())) {
                    for (final var plant : response.data()) {
                        final var imageUrl = extractThumbnailUrl(plant);
                        if (Objects.nonNull(imageUrl)) {
                            log.info("Found Perenual image for '{}' (searched '{}'): {}", plantName, query, imageUrl);
                            return imageUrl;
                        }
                    }
                }
            } catch (final Exception e) {
                log.debug("Failed to look up plant image for '{}': {}", query, e.getMessage());
            }
        }

        log.info("No image found for '{}'", plantName);
        return null;
    }

    /**
     * Simplifies a plant name for better Perenual search results.
     * Strips cultivar names (after dash/quotes), prefixes like "Dwarf"/"Ornamental Grass -", etc.
     */
    private String simplifyPlantName(final String name) {
        var simplified = name;

        // Remove everything after a dash (cultivar names like "Karl Foerster")
        if (simplified.contains(" - ")) {
            simplified = simplified.substring(0, simplified.indexOf(" - "));
        }

        // Remove common prefixes that narrow results too much
        for (final var prefix : new String[] {
            "Dwarf ", "Ornamental ", "Japanese ", "Chinese ", "Korean ", "Knock Out ", "Drift ", "Encore "
        }) {
            if (simplified.startsWith(prefix)) {
                simplified = simplified.substring(prefix.length());
            }
        }

        // Remove anything in quotes or parentheses
        simplified = simplified
                .replaceAll("['\"].*?['\"]", "")
                .replaceAll("\\(.*?\\)", "")
                .trim();

        return simplified.equals(name) ? "" : simplified;
    }

    /**
     * Extracts the best available thumbnail URL from a Perenual plant record.
     * Filters out the "upgrade_access.jpg" placeholder that Perenual returns for paywalled plants.
     */
    private String extractThumbnailUrl(final PerenualPlant plant) {
        final var image = plant.defaultImage();
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
}
