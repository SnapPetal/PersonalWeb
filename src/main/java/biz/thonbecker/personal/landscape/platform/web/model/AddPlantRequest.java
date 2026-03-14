package biz.thonbecker.personal.landscape.platform.web.model;

/**
 * Request DTO for adding a plant placement to a plan.
 *
 * @param usdaSymbol USDA plant symbol
 * @param plantName Scientific plant name (optional, avoids USDA API lookup)
 * @param commonName Common plant name (optional, avoids USDA API lookup)
 * @param xCoord X coordinate (percentage 0-100)
 * @param yCoord Y coordinate (percentage 0-100)
 * @param notes Optional notes
 */
public record AddPlantRequest(
        String usdaSymbol, String plantName, String commonName, double xCoord, double yCoord, String notes) {}
