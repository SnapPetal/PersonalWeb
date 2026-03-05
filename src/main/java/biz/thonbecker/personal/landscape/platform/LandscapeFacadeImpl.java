package biz.thonbecker.personal.landscape.platform;

import biz.thonbecker.personal.landscape.api.*;
import biz.thonbecker.personal.landscape.domain.exceptions.PlanNotFoundException;
import biz.thonbecker.personal.landscape.platform.persistence.*;
import biz.thonbecker.personal.landscape.platform.service.LandscapeAiService;
import biz.thonbecker.personal.landscape.platform.service.LandscapeImageStorageService;
import biz.thonbecker.personal.landscape.platform.service.PlantApiService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the Landscape Planning facade.
 *
 * <p>Coordinates image storage, AI analysis, plant search, and plan persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandscapeFacadeImpl implements LandscapeFacade {

    private final LandscapeImageStorageService imageStorageService;
    private final LandscapeAiService aiService;
    private final PlantApiService plantApiService;
    private final LandscapePlanRepository planRepository;
    private final PlantPlacementRepository placementRepository;

    @Override
    @Transactional
    public LandscapePlan createPlan(
            final String userId,
            final String name,
            final String description,
            final byte[] imageData,
            final HardinessZone zone,
            final String zipCode) {

        log.info("Creating landscape plan for user {} with zone {}", userId, zone);

        // Step 1: Upload image to S3
        final var uploadResult = imageStorageService.store(imageData, "image/jpeg", userId);

        // Step 2: Generate AI recommendations
        final var aiRecommendations = aiService.analyzeImageAndRecommendPlants(imageData, zone, description);

        // Step 3: Save plan entity
        final var planEntity = new LandscapePlanEntity();
        planEntity.setUserId(userId);
        planEntity.setName(name);
        planEntity.setDescription(description);
        planEntity.setImageS3Key(uploadResult.s3Key());
        planEntity.setImageCdnUrl(uploadResult.cdnUrl());
        planEntity.setHardinessZone(zone.name());
        planEntity.setZipCode(zipCode);

        final var savedPlan = planRepository.save(planEntity);

        // Step 4: Save AI recommendations
        for (final var rec : aiRecommendations) {
            final var recEntity = new RecommendedPlantEntity();
            recEntity.setPlan(savedPlan);
            recEntity.setUsdaSymbol(rec.usdaSymbol());
            recEntity.setPlantName(rec.scientificName());
            recEntity.setCommonName(rec.commonName());
            recEntity.setRecommendationReason(rec.recommendationReason());
            recEntity.setConfidenceScore(rec.confidenceScore());
            recEntity.setLightRequirement(rec.lightRequirement().name());
            recEntity.setWaterRequirement(rec.waterRequirement().name());
            savedPlan.getRecommendations().add(recEntity);
        }

        planRepository.save(savedPlan);

        log.info(
                "Successfully created landscape plan {} with {} recommendations",
                savedPlan.getId(),
                aiRecommendations.size());

        return convertToDomain(savedPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlantInfo> searchPlants(
            final String query,
            final HardinessZone zone,
            final LightRequirement lightRequirement,
            final WaterRequirement waterRequirement) {

        log.debug("Searching plants: query={}, zone={}", query, zone);
        return plantApiService.searchPlants(query, zone, lightRequirement, waterRequirement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlantInfo> getRecommendations(final Long planId) {
        log.debug("Fetching recommendations for plan {}", planId);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        final var recommendations = new ArrayList<PlantInfo>();
        for (final var rec : plan.getRecommendations()) {
            recommendations.add(new PlantInfo(
                    rec.getUsdaSymbol(),
                    rec.getPlantName(),
                    rec.getCommonName(),
                    null, // Family name not stored in recommendations
                    List.of(), // Hardiness zones not stored
                    Objects.nonNull(rec.getLightRequirement())
                            ? LightRequirement.valueOf(rec.getLightRequirement())
                            : null,
                    Objects.nonNull(rec.getWaterRequirement())
                            ? WaterRequirement.valueOf(rec.getWaterRequirement())
                            : null,
                    null, // Category not stored
                    null, // Native status not stored
                    null, // Height not stored
                    null)); // Width not stored
        }

        return recommendations;
    }

    @Override
    @Transactional
    public void addPlantPlacement(
            final Long planId, final String usdaSymbol, final double x, final double y, final String notes) {

        log.info("Adding plant placement to plan {}: {}", planId, usdaSymbol);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        // Fetch plant details from USDA API
        final var plantInfo = plantApiService.getPlantDetails(usdaSymbol);

        final var placement = new PlantPlacementEntity();
        placement.setPlan(plan);
        placement.setUsdaSymbol(usdaSymbol);
        placement.setPlantName(plantInfo.scientificName());
        placement.setCommonName(plantInfo.commonName());
        placement.setXCoord(BigDecimal.valueOf(x));
        placement.setYCoord(BigDecimal.valueOf(y));
        placement.setNotes(notes);
        placement.setQuantity(1);

        placementRepository.save(placement);

        log.info("Successfully added plant placement for plan {}", planId);
    }

    @Override
    @Transactional(readOnly = true)
    public LandscapePlan getPlan(final Long planId) {
        log.debug("Fetching landscape plan {}", planId);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        return convertToDomain(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandscapePlan> getUserPlans(final String userId) {
        log.debug("Fetching landscape plans for user {}", userId);

        final var plans = planRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return plans.stream().map(this::convertToDomain).toList();
    }

    @Override
    @Transactional
    public void deletePlan(final Long planId) {
        log.info("Deleting landscape plan {}", planId);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        planRepository.delete(plan);

        log.info("Successfully deleted landscape plan {}", planId);
    }

    /**
     * Converts a JPA entity to the domain model.
     *
     * @param entity JPA entity
     * @return Domain landscape plan
     */
    private LandscapePlan convertToDomain(final LandscapePlanEntity entity) {
        final var placements = entity.getPlacements().stream()
                .map(p -> new PlantPlacement(
                        p.getId(),
                        p.getUsdaSymbol(),
                        p.getPlantName(),
                        p.getCommonName(),
                        p.getXCoord().doubleValue(),
                        p.getYCoord().doubleValue(),
                        p.getNotes(),
                        p.getQuantity(),
                        p.getCreatedAt()))
                .toList();

        final var recommendations = entity.getRecommendations().stream()
                .map(r -> new PlantInfo(
                        r.getUsdaSymbol(),
                        r.getPlantName(),
                        r.getCommonName(),
                        null,
                        List.of(),
                        Objects.nonNull(r.getLightRequirement())
                                ? LightRequirement.valueOf(r.getLightRequirement())
                                : null,
                        Objects.nonNull(r.getWaterRequirement())
                                ? WaterRequirement.valueOf(r.getWaterRequirement())
                                : null,
                        null,
                        null,
                        null,
                        null))
                .toList();

        return new LandscapePlan(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getDescription(),
                entity.getImageCdnUrl(),
                HardinessZone.valueOf(entity.getHardinessZone()),
                entity.getZipCode(),
                placements,
                recommendations,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
