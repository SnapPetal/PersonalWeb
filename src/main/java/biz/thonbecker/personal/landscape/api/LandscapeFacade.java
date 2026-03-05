package biz.thonbecker.personal.landscape.api;

import java.util.List;

/**
 * Public facade for the Landscape Planning module.
 *
 * <p>This is the ONLY entry point for other modules to interact with landscape planning
 * functionality.
 */
public interface LandscapeFacade {

    /**
     * Creates a new landscape plan with an uploaded image.
     *
     * <p>The image is uploaded to S3, analyzed by AI for plant recommendations, and persisted to
     * the database.
     *
     * @param userId User creating the plan
     * @param name Plan name
     * @param description Optional plan description
     * @param imageData Raw image bytes (JPEG or PNG)
     * @param zone USDA hardiness zone for this location
     * @param zipCode Optional zip code
     * @return The created landscape plan with AI recommendations
     */
    LandscapePlan createPlan(
            String userId, String name, String description, byte[] imageData, HardinessZone zone, String zipCode);

    /**
     * Searches for plants matching the given criteria.
     *
     * <p>Results are filtered by hardiness zone and optionally by light and water requirements.
     * Results are cached for 24 hours.
     *
     * @param query Plant name search query
     * @param zone Required hardiness zone
     * @param lightRequirement Optional light requirement filter
     * @param waterRequirement Optional water requirement filter
     * @return List of matching plants
     */
    List<PlantInfo> searchPlants(
            String query, HardinessZone zone, LightRequirement lightRequirement, WaterRequirement waterRequirement);

    /**
     * Retrieves AI-generated plant recommendations for a plan.
     *
     * @param planId Plan identifier
     * @return List of recommended plants with reasoning
     */
    List<PlantInfo> getRecommendations(Long planId);

    /**
     * Adds a plant placement to an existing plan.
     *
     * @param planId Plan identifier
     * @param usdaSymbol USDA plant symbol
     * @param x X coordinate on image (percentage 0-100)
     * @param y Y coordinate on image (percentage 0-100)
     * @param notes Optional placement notes
     */
    void addPlantPlacement(Long planId, String usdaSymbol, double x, double y, String notes);

    /**
     * Retrieves a specific landscape plan by ID.
     *
     * @param planId Plan identifier
     * @return The landscape plan with all placements and recommendations
     */
    LandscapePlan getPlan(Long planId);

    /**
     * Retrieves all landscape plans for a user.
     *
     * @param userId User identifier
     * @return List of user's landscape plans
     */
    List<LandscapePlan> getUserPlans(String userId);

    /**
     * Deletes a landscape plan and all associated data.
     *
     * @param planId Plan identifier
     */
    void deletePlan(Long planId);
}
