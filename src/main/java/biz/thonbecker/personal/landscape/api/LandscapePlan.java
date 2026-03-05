package biz.thonbecker.personal.landscape.api;

import java.time.Instant;
import java.util.List;

/**
 * A landscape plan created by a user.
 *
 * <p>Contains the uploaded image, user's hardiness zone, plant placements, and AI-generated
 * recommendations.
 *
 * @param id Database identifier, null for new plans
 * @param userId User who owns this plan
 * @param name Plan name
 * @param description Optional plan description
 * @param imageCdnUrl CDN URL of the uploaded landscape image
 * @param hardinessZone USDA hardiness zone for this location
 * @param zipCode Optional zip code
 * @param placements User-placed plants on the image
 * @param recommendations AI-generated plant recommendations
 * @param createdAt When the plan was created
 * @param updatedAt When the plan was last modified
 */
public record LandscapePlan(
        Long id,
        String userId,
        String name,
        String description,
        String imageCdnUrl,
        HardinessZone hardinessZone,
        String zipCode,
        List<PlantPlacement> placements,
        List<PlantInfo> recommendations,
        Instant createdAt,
        Instant updatedAt) {}
