package biz.thonbecker.personal.landscape.api;

import java.time.Instant;

/**
 * Represents a plant placed by the user on their landscape plan.
 *
 * @param id Database identifier, null for new placements
 * @param usdaSymbol USDA plant symbol
 * @param plantName Scientific plant name
 * @param commonName Common plant name
 * @param xCoord X coordinate on image (percentage 0-100)
 * @param yCoord Y coordinate on image (percentage 0-100)
 * @param notes User notes about this placement
 * @param quantity Number of plants at this location
 * @param createdAt When this placement was created
 */
public record PlantPlacement(
        Long id,
        String usdaSymbol,
        String plantName,
        String commonName,
        double xCoord,
        double yCoord,
        String notes,
        int quantity,
        Instant createdAt) {}
