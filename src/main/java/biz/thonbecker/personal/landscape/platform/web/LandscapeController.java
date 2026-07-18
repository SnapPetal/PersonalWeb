package biz.thonbecker.personal.landscape.platform.web;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LandscapePlan;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.SeasonalAnalysis;
import biz.thonbecker.personal.landscape.api.WaterRequirement;
import biz.thonbecker.personal.landscape.platform.LandscapeService;
import biz.thonbecker.personal.landscape.platform.web.model.AddPlantRequest;
import biz.thonbecker.personal.user.api.UserSessionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    private final LandscapeService landscapeService;
    private final UserSessionResolver userSessionResolver;

    /**
     * Main landscape planner page.
     *
     * @param model Spring MVC model
     * @param request HTTP request
     * @param response HTTP response
     * @return Thymeleaf template name
     */
    @GetMapping
    public String landscapePlanner(
            final Model model, final HttpServletRequest request, final HttpServletResponse response) {
        final var ownerId = findOwnerId(request).orElse(null);
        if (ownerId == null) {
            return "redirect:/auth/login";
        }
        final var userPlans = landscapeService.getUserPlans(ownerId);
        model.addAttribute("userPlans", userPlans);
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
     * @param request HTTP request
     * @param response HTTP response
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
            final HttpServletRequest request,
            final HttpServletResponse response) {

        if (image.isEmpty()) {
            log.warn("Empty image uploaded for plan creation");
            return ResponseEntity.badRequest().build();
        }

        try {
            // CRITICAL: Copy bytes immediately before MultipartFile cleanup
            final var imageBytes = image.getBytes();
            final var userId = requireOwnerId(request);

            log.info("Creating landscape plan for user: {}, zone: {}", userId, hardinessZone);

            final var plan = landscapeService.createPlan(userId, name, description, imageBytes, hardinessZone, zipCode);

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
    public ResponseEntity<LandscapePlan> getPlan(
            @PathVariable final Long planId, final HttpServletRequest request, final HttpServletResponse response) {
        try {
            final var ownerId = requireOwnerId(request);
            final var plan = landscapeService.getPlan(planId, ownerId);
            return ResponseEntity.ok(plan);
        } catch (final Exception e) {
            log.error("Failed to retrieve plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lists all plans for the current browser owner.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @return List of user's plans
     */
    @GetMapping("/plans")
    @ResponseBody
    public ResponseEntity<List<LandscapePlan>> getUserPlans(
            final HttpServletRequest request, final HttpServletResponse response) {
        final var ownerId = requireOwnerId(request);
        final var plans = landscapeService.getUserPlans(ownerId);
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
            final var plants = landscapeService.searchPlants(query, zone, lightRequirement, waterRequirement);
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
    public String getRecommendations(
            @PathVariable final Long planId,
            final Model model,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        try {
            final var ownerId = requireOwnerId(request);
            final var recommendations = landscapeService.getRecommendations(planId, ownerId);
            model.addAttribute("recommendations", recommendations);
            log.info("Retrieved {} recommendations for plan {}", recommendations.size(), planId);
        } catch (final Exception e) {
            log.error("Failed to get recommendations for plan {}: {}", planId, e.getMessage(), e);
            model.addAttribute("recommendations", List.of());
            model.addAttribute(
                    "error",
                    e.getMessage() != null && e.getMessage().contains("rate limit")
                            ? e.getMessage()
                            : "Failed to load recommendations.");
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
    public ResponseEntity<java.util.Map<String, Long>> addPlantPlacement(
            @PathVariable final Long planId,
            @RequestBody final AddPlantRequest request,
            final HttpServletRequest httpRequest,
            final HttpServletResponse response) {

        try {
            log.info("Adding plant placement to plan {}: {}", planId, request.usdaSymbol());
            final var ownerId = requireOwnerId(httpRequest);
            final var placementId = landscapeService.addPlantPlacement(
                    planId,
                    ownerId,
                    request.usdaSymbol(),
                    request.plantName(),
                    request.commonName(),
                    request.xCoord(),
                    request.yCoord(),
                    request.notes());
            return ResponseEntity.ok(java.util.Map.of("id", placementId));
        } catch (final Exception e) {
            log.error("Failed to add plant placement to plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Removes a plant placement from a plan.
     *
     * @param planId Plan identifier
     * @param placementId Placement identifier
     * @return Success response
     */
    @DeleteMapping("/plans/{planId}/placements/{placementId}")
    @ResponseBody
    public ResponseEntity<Void> removePlantPlacement(
            @PathVariable final Long planId,
            @PathVariable final Long placementId,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        try {
            log.info("Removing plant placement {} from plan {}", placementId, planId);
            final var ownerId = requireOwnerId(request);
            landscapeService.removePlantPlacement(planId, placementId, ownerId);
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to remove plant placement {} from plan {}: {}", placementId, planId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Fetches a plant image URL from the plant data provider.
     *
     * @param symbol Plant symbol identifier
     * @param name Optional common name (used for Perenual search since USDA symbols don't work there)
     * @return JSON with imageUrl field pointing to the proxy endpoint
     */
    @GetMapping("/plants/image")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, String>> getPlantImage(
            @RequestParam("symbol") final String symbol,
            @RequestParam(value = "name", required = false) final String name) {
        try {
            // Prefer searching by common name if available (Perenual doesn't understand USDA symbols)
            final var searchTerm = (name != null && !name.isBlank()) ? name : symbol;
            final var imageUrl = landscapeService.getPlantImageUrl(searchTerm);
            if (imageUrl != null) {
                return ResponseEntity.ok(java.util.Map.of(
                        "imageUrl",
                        "/landscape/plants/image/proxy?url=" + java.net.URLEncoder.encode(imageUrl, "UTF-8")));
            }
            return ResponseEntity.notFound().build();
        } catch (final Exception e) {
            log.error("Failed to fetch plant image for '{}': {}", name != null ? name : symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Proxies a plant image to avoid CORS issues when loading images onto Fabric.js canvas.
     *
     * @param url The external image URL to proxy
     * @return Proxied image bytes with appropriate content type
     */
    @GetMapping("/plants/image/proxy")
    public ResponseEntity<byte[]> proxyPlantImage(@RequestParam("url") final String url) {
        try {
            final var uri = java.net.URI.create(url);
            final var request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            final var response = java.net.http.HttpClient.newHttpClient()
                    .send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                return ResponseEntity.notFound().build();
            }

            final var contentType =
                    response.headers().firstValue("Content-Type").orElse("image/jpeg");

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Cache-Control", "public, max-age=86400")
                    .body(response.body());
        } catch (final Exception e) {
            log.error("Failed to proxy plant image for '{}': {}", url, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generates a seasonal analysis for a landscape plan.
     *
     * @param planId Plan identifier
     * @return Seasonal analysis with descriptions for each season
     */
    @GetMapping("/plans/{planId}/seasons")
    @ResponseBody
    public ResponseEntity<SeasonalAnalysis> getSeasonalAnalysis(
            @PathVariable final Long planId, final HttpServletRequest request, final HttpServletResponse response) {
        try {
            log.info("Generating seasonal analysis for plan {}", planId);
            final var ownerId = requireOwnerId(request);
            final var analysis = landscapeService.getSeasonalAnalysis(planId, ownerId);
            return ResponseEntity.ok(analysis);
        } catch (final Exception e) {
            log.error("Failed to generate seasonal analysis for plan {}: {}", planId, e.getMessage(), e);
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
    public ResponseEntity<Void> deletePlan(
            @PathVariable final Long planId, final HttpServletRequest request, final HttpServletResponse response) {
        try {
            log.info("Deleting landscape plan {}", planId);
            final var ownerId = requireOwnerId(request);
            landscapeService.deletePlan(planId, ownerId);
            return ResponseEntity.ok().build();
        } catch (final Exception e) {
            log.error("Failed to delete plan {}: {}", planId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String requireOwnerId(final HttpServletRequest request) {
        return findOwnerId(request).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private java.util.Optional<String> findOwnerId(final HttpServletRequest request) {
        final var cookies = request.getCookies() == null ? new jakarta.servlet.http.Cookie[0] : request.getCookies();
        return java.util.Arrays.stream(cookies)
                .filter(cookie -> UserSessionResolver.SESSION_COOKIE_NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .flatMap(userSessionResolver::resolveUserId);
    }
}
