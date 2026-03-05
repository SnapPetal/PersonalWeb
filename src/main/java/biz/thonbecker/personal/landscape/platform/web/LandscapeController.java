package biz.thonbecker.personal.landscape.platform.web;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LandscapeFacade;
import biz.thonbecker.personal.landscape.api.LandscapePlan;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.WaterRequirement;
import biz.thonbecker.personal.landscape.platform.web.model.AddPlantRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Web controller for landscape planning features.
 *
 * <p>Provides endpoints for plan creation, plant search, and plan management.
 */
@Controller
@RequestMapping("/landscape")
@RequiredArgsConstructor
@Slf4j
public class LandscapeController {

    private final LandscapeFacade landscapeFacade;

    /**
     * Main landscape planner page.
     *
     * @param model Spring MVC model
     * @param principal Authenticated user
     * @return Thymeleaf template name
     */
    @GetMapping
    public String landscapePlanner(final Model model, final Principal principal) {
        if (principal != null) {
            final var userPlans = landscapeFacade.getUserPlans(principal.getName());
            model.addAttribute("userPlans", userPlans);
        }
        model.addAttribute("hardinessZones", HardinessZone.values());
        return "landscape/planner";
    }

    /**
     * Creates a new landscape plan with uploaded image.
     *
     * @param image Uploaded image file
     * @param name Plan name
     * @param description Optional description
     * @param hardinessZone Hardiness zone
     * @param zipCode Optional zip code
     * @param principal Authenticated user
     * @return Created plan
     */
    @PostMapping("/plans")
    @ResponseBody
    public ResponseEntity<LandscapePlan> createPlan(
            @RequestParam("image") final MultipartFile image,
            @RequestParam("name") final String name,
            @RequestParam(value = "description", required = false) final String description,
            @RequestParam("hardinessZone") final HardinessZone hardinessZone,
            @RequestParam(value = "zipCode", required = false) final String zipCode,
            final Principal principal) {

        if (image.isEmpty()) {
            log.warn("Empty image uploaded for plan creation");
            return ResponseEntity.badRequest().build();
        }

        if (principal == null) {
            log.warn("Unauthenticated user attempted to create plan");
            return ResponseEntity.status(401).build();
        }

        try {
            // CRITICAL: Copy bytes immediately before MultipartFile cleanup
            final var imageBytes = image.getBytes();
            final var userId = principal.getName();

            log.info("Creating landscape plan for user: {}, zone: {}", userId, hardinessZone);

            final var plan = landscapeFacade.createPlan(userId, name, description, imageBytes, hardinessZone, zipCode);

            log.info("Successfully created plan {} for user {}", plan.id(), userId);
            return ResponseEntity.ok(plan);

        } catch (final IOException e) {
            log.error("Failed to read uploaded image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (final Exception e) {
            log.error("Failed to create landscape plan: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves a specific landscape plan.
     *
     * @param planId Plan identifier
     * @return Landscape plan
     */
    @GetMapping("/plans/{planId}")
    @ResponseBody
    public ResponseEntity<LandscapePlan> getPlan(@PathVariable final Long planId) {
        try {
            final var plan = landscapeFacade.getPlan(planId);
            return ResponseEntity.ok(plan);
        } catch (final Exception e) {
            log.error("Failed to retrieve plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lists all plans for the authenticated user.
     *
     * @param principal Authenticated user
     * @return List of user's plans
     */
    @GetMapping("/plans")
    @ResponseBody
    public ResponseEntity<List<LandscapePlan>> getUserPlans(final Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        final var plans = landscapeFacade.getUserPlans(principal.getName());
        return ResponseEntity.ok(plans);
    }

    /**
     * Searches for plants matching criteria (HTMX endpoint).
     *
     * @param query Search query
     * @param zone Hardiness zone
     * @param lightRequirement Optional light filter
     * @param waterRequirement Optional water filter
     * @param model Spring MVC model
     * @return Thymeleaf fragment with search results
     */
    @GetMapping("/plants/search")
    public String searchPlants(
            @RequestParam(value = "query", required = false) final String query,
            @RequestParam(value = "zone", required = false) final HardinessZone zone,
            @RequestParam(value = "lightRequirement", required = false) final LightRequirement lightRequirement,
            @RequestParam(value = "waterRequirement", required = false) final WaterRequirement waterRequirement,
            final Model model) {

        if (query == null || query.isBlank() || zone == null) {
            model.addAttribute("plants", List.of());
            return "landscape/fragments :: search-results";
        }

        try {
            log.debug("Searching plants: query={}, zone={}", query, zone);
            final var plants = landscapeFacade.searchPlants(query, zone, lightRequirement, waterRequirement);
            model.addAttribute("plants", plants);
            log.info("Found {} plants matching search criteria", plants.size());
        } catch (final Exception e) {
            log.error("Plant search failed: {}", e.getMessage(), e);
            model.addAttribute("plants", List.of());
            model.addAttribute("error", "Failed to search plants. Please try again.");
        }

        return "landscape/fragments :: search-results";
    }

    /**
     * Retrieves AI recommendations for a plan (HTMX endpoint).
     *
     * @param planId Plan identifier
     * @param model Spring MVC model
     * @return Thymeleaf fragment with recommendations
     */
    @GetMapping("/plans/{planId}/recommendations")
    public String getRecommendations(@PathVariable final Long planId, final Model model) {
        try {
            final var recommendations = landscapeFacade.getRecommendations(planId);
            model.addAttribute("recommendations", recommendations);
            log.info("Retrieved {} recommendations for plan {}", recommendations.size(), planId);
        } catch (final Exception e) {
            log.error("Failed to get recommendations for plan {}: {}", planId, e.getMessage(), e);
            model.addAttribute("recommendations", List.of());
            model.addAttribute("error", "Failed to load recommendations.");
        }

        return "landscape/fragments :: recommendations";
    }

    /**
     * Adds a plant placement to a plan.
     *
     * @param planId Plan identifier
     * @param request Placement details
     * @return Success response
     */
    @PostMapping("/plans/{planId}/placements")
    @ResponseBody
    public ResponseEntity<Void> addPlantPlacement(
            @PathVariable final Long planId, @RequestBody final AddPlantRequest request) {

        try {
            log.info("Adding plant placement to plan {}: {}", planId, request.usdaSymbol());
            landscapeFacade.addPlantPlacement(
                    planId, request.usdaSymbol(), request.xCoord(), request.yCoord(), request.notes());
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to add plant placement to plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Deletes a landscape plan.
     *
     * @param planId Plan identifier
     * @return Success response
     */
    @DeleteMapping("/plans/{planId}")
    @ResponseBody
    public ResponseEntity<Void> deletePlan(@PathVariable final Long planId) {
        try {
            log.info("Deleting landscape plan {}", planId);
            landscapeFacade.deletePlan(planId);
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to delete plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
