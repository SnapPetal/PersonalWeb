package biz.thonbecker.personal.landscape.platform;

import biz.thonbecker.personal.landscape.api.*;
import biz.thonbecker.personal.landscape.domain.exceptions.PlanNotFoundException;
import biz.thonbecker.personal.landscape.platform.persistence.*;
import biz.thonbecker.personal.landscape.platform.service.LandscapeAiService;
import biz.thonbecker.personal.landscape.platform.service.LandscapeImageGenerationService;
import biz.thonbecker.personal.landscape.platform.service.LandscapeImageStorageService;
import biz.thonbecker.personal.landscape.platform.service.PlantApiService;
import biz.thonbecker.personal.landscape.platform.service.PlantImageService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Landscape planning service coordinating image storage, AI analysis, plant search, and plan persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandscapeService {

    private final LandscapeImageStorageService imageStorageService;
    private final LandscapeAiService aiService;
    private final LandscapeImageGenerationService imageGenerationService;
    private final PlantImageService plantImageService;
    private final PlantApiService plantApiService;
    private final LandscapePlanRepository planRepository;
    private final PlantPlacementRepository placementRepository;
    private final S3Client s3Client;

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
        final var uploadResult = imageStorageService.store(imageData, detectImageContentType(imageData), userId);

        // Step 2: Save plan entity
        final var planEntity = new LandscapePlanEntity();
        planEntity.setUserId(userId);
        planEntity.setName(name);
        planEntity.setDescription(description);
        planEntity.setImageS3Key(uploadResult.s3Key());
        planEntity.setImageCdnUrl(uploadResult.cdnUrl());
        planEntity.setHardinessZone(zone.name());
        planEntity.setZipCode(zipCode);

        final var savedPlan = planRepository.save(planEntity);
        addAiRecommendations(savedPlan, imageData, zone, description);

        log.info("Successfully created landscape plan {}", savedPlan.getId());

        return convertToDomain(savedPlan);
    }

    @Transactional(readOnly = true)
    public List<PlantInfo> searchPlants(
            final String query,
            final HardinessZone zone,
            final LightRequirement lightRequirement,
            final WaterRequirement waterRequirement) {

        log.debug("Searching plants: query={}, zone={}", query, zone);
        return plantApiService.searchPlants(query, zone, lightRequirement, waterRequirement);
    }

    @Transactional
    public List<PlantInfo> getRecommendations(final Long planId) {
        log.debug("Fetching plant recommendations for plan {}", planId);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        if (!plan.getRecommendations().isEmpty()) {
            return plan.getRecommendations().stream()
                    .map(this::convertRecommendation)
                    .toList();
        }

        try {
            final var imageData = fetchPlanImage(plan);
            addAiRecommendations(
                    plan, imageData, HardinessZone.valueOf(plan.getHardinessZone()), plan.getDescription());
            if (!plan.getRecommendations().isEmpty()) {
                return plan.getRecommendations().stream()
                        .map(this::convertRecommendation)
                        .toList();
            }
        } catch (final Exception e) {
            log.warn("Failed to generate AI recommendations for plan {}, falling back to zone plants", planId, e);
        }

        return plantApiService.getPlantsByZone(HardinessZone.valueOf(plan.getHardinessZone()));
    }

    @Transactional
    public Long addPlantPlacement(
            final Long planId,
            final String usdaSymbol,
            final String plantName,
            final String commonName,
            final double x,
            final double y,
            final String notes) {

        log.info("Adding plant placement to plan {}: {}", planId, usdaSymbol);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        // Use provided names; only call USDA API as fallback
        var resolvedPlantName = plantName;
        var resolvedCommonName = commonName;
        if (Objects.isNull(resolvedPlantName) || resolvedPlantName.isBlank()) {
            final var plantInfo = plantApiService.getPlantDetails(usdaSymbol);
            resolvedPlantName = plantInfo.scientificName();
            resolvedCommonName = plantInfo.commonName();
        }

        final var placement = new PlantPlacementEntity();
        placement.setPlan(plan);
        placement.setUsdaSymbol(usdaSymbol);
        placement.setPlantName(resolvedPlantName);
        placement.setCommonName(resolvedCommonName);
        placement.setXCoord(BigDecimal.valueOf(x));
        placement.setYCoord(BigDecimal.valueOf(y));
        placement.setNotes(notes);
        placement.setQuantity(1);

        final var saved = placementRepository.save(placement);

        log.info("Successfully added plant placement {} for plan {}", saved.getId(), planId);
        return saved.getId();
    }

    @Transactional
    public void removePlantPlacement(final Long planId, final Long placementId) {
        log.info("Removing plant placement {} from plan {}", placementId, planId);

        final var placement = placementRepository
                .findById(placementId)
                .orElseThrow(() -> new IllegalArgumentException("Placement not found: " + placementId));

        if (!placement.getPlan().getId().equals(planId)) {
            throw new IllegalArgumentException("Placement " + placementId + " does not belong to plan " + planId);
        }

        placementRepository.delete(placement);

        log.info("Successfully removed plant placement {} from plan {}", placementId, planId);
    }

    @Transactional(readOnly = true)
    public LandscapePlan getPlan(final Long planId) {
        log.debug("Fetching landscape plan {}", planId);

        final var plan =
                planRepository.findByIdWithPlacements(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        return convertToDomain(plan);
    }

    @Transactional(readOnly = true)
    public List<LandscapePlan> getUserPlans(final String userId) {
        log.debug("Fetching landscape plans for user {}", userId);

        final var plans = planRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return plans.stream().map(this::convertToDomain).toList();
    }

    @Transactional
    public void deletePlan(final Long planId) {
        log.info("Deleting landscape plan {}", planId);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        planRepository.delete(plan);

        log.info("Successfully deleted landscape plan {}", planId);
    }

    @Transactional(readOnly = true)
    public SeasonalAnalysis getSeasonalAnalysis(final Long planId) {
        log.info("Generating seasonal analysis for plan {}", planId);

        final var plan =
                planRepository.findByIdWithPlacements(planId).orElseThrow(() -> new PlanNotFoundException(planId));

        final var plantDescriptions = plan.getPlacements().stream()
                .map(p -> {
                    final var name = Objects.nonNull(p.getCommonName()) ? p.getCommonName() : p.getPlantName();
                    return name + " (" + p.getUsdaSymbol() + ")";
                })
                .toList();

        final var plantPlacementPrompts = plan.getPlacements().stream()
                .map(p -> {
                    final var name = Objects.nonNull(p.getCommonName()) ? p.getCommonName() : p.getPlantName();
                    return new LandscapeImageGenerationService.PlantPlacementPrompt(
                            p.getUsdaSymbol(),
                            name,
                            p.getXCoord().doubleValue(),
                            p.getYCoord().doubleValue());
                })
                .toList();

        try {
            final var imageData = fetchPlanImage(plan);

            // Get text descriptions from OpenAI
            final var textAnalysis = aiService.analyzeSeasons(
                    imageData, HardinessZone.valueOf(plan.getHardinessZone()), plantDescriptions);

            final var springImage =
                    imageGenerationService.generateSeasonalImage(imageData, "spring", plantPlacementPrompts);
            final var summerImage =
                    imageGenerationService.generateSeasonalImage(imageData, "summer", plantPlacementPrompts);
            final var fallImage =
                    imageGenerationService.generateSeasonalImage(imageData, "fall", plantPlacementPrompts);
            final var winterImage =
                    imageGenerationService.generateSeasonalImage(imageData, "winter", plantPlacementPrompts);

            return new SeasonalAnalysis(
                    mergeWithImage(textAnalysis.spring(), springImage),
                    mergeWithImage(textAnalysis.summer(), summerImage),
                    mergeWithImage(textAnalysis.fall(), fallImage),
                    mergeWithImage(textAnalysis.winter(), winterImage));

        } catch (final Exception e) {
            log.error("Failed to generate seasonal analysis for plan {}: {}", planId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate seasonal analysis", e);
        }
    }

    private void addAiRecommendations(
            final LandscapePlanEntity plan,
            final byte[] imageData,
            final HardinessZone zone,
            final String description) {
        final var recommendations = aiService.analyzeImageAndRecommendPlants(imageData, zone, description);
        plan.getRecommendations().clear();
        recommendations.stream()
                .map(recommendation -> toRecommendedPlantEntity(plan, recommendation))
                .forEach(plan.getRecommendations()::add);
        log.info("Stored {} AI plant recommendations for plan {}", recommendations.size(), plan.getId());
    }

    private static RecommendedPlantEntity toRecommendedPlantEntity(
            final LandscapePlanEntity plan, final LandscapeAiService.PlantRecommendation recommendation) {
        final var entity = new RecommendedPlantEntity();
        entity.setPlan(plan);
        entity.setUsdaSymbol(firstNonBlank(recommendation.usdaSymbol(), recommendation.scientificName(), "UNKNOWN"));
        entity.setPlantName(
                firstNonBlank(recommendation.scientificName(), recommendation.commonName(), "Unknown plant"));
        entity.setCommonName(
                firstNonBlank(recommendation.commonName(), recommendation.scientificName(), "Unknown plant"));
        entity.setRecommendationReason(firstNonBlank(
                recommendation.recommendationReason(), "Recommended based on the visible landscape conditions."));
        entity.setConfidenceScore(Math.max(0, Math.min(100, recommendation.confidenceScore())));
        entity.setLightRequirement(
                Objects.nonNull(recommendation.lightRequirement())
                        ? recommendation.lightRequirement().name()
                        : null);
        entity.setWaterRequirement(
                Objects.nonNull(recommendation.waterRequirement())
                        ? recommendation.waterRequirement().name()
                        : null);
        return entity;
    }

    private static String firstNonBlank(final String... values) {
        for (final var value : values) {
            if (Objects.nonNull(value) && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private byte[] fetchPlanImage(final LandscapePlanEntity plan) throws java.io.IOException {
        final var getRequest = GetObjectRequest.builder()
                .bucket(imageStorageService.getBucketName())
                .key(plan.getImageS3Key())
                .build();

        try (final ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest)) {
            return s3Object.readAllBytes();
        }
    }

    private static String detectImageContentType(final byte[] imageData) {
        if (imageData.length >= 8
                && imageData[0] == (byte) 0x89
                && imageData[1] == 0x50
                && imageData[2] == 0x4E
                && imageData[3] == 0x47
                && imageData[4] == 0x0D
                && imageData[5] == 0x0A
                && imageData[6] == 0x1A
                && imageData[7] == 0x0A) {
            return "image/png";
        }
        return "image/jpeg";
    }

    public String getPlantImageUrl(final String usdaSymbol) {
        return plantImageService.getPlantImageUrl(usdaSymbol);
    }

    private SeasonalAnalysis.SeasonalDescription mergeWithImage(
            final SeasonalAnalysis.SeasonalDescription description, final String imageBase64) {
        if (Objects.isNull(description)) {
            return null;
        }
        return new SeasonalAnalysis.SeasonalDescription(description.description(), description.careTips(), imageBase64);
    }

    private PlantInfo convertRecommendation(final RecommendedPlantEntity rec) {
        final var imageUrl = plantImageService.getPlantImageUrl(
                firstNonBlank(rec.getCommonName(), rec.getPlantName(), rec.getUsdaSymbol()));

        return new PlantInfo(
                rec.getUsdaSymbol(),
                rec.getPlantName(),
                rec.getCommonName(),
                null,
                List.of(),
                Objects.nonNull(rec.getLightRequirement()) ? LightRequirement.valueOf(rec.getLightRequirement()) : null,
                Objects.nonNull(rec.getWaterRequirement()) ? WaterRequirement.valueOf(rec.getWaterRequirement()) : null,
                null,
                null,
                null,
                null,
                imageUrl,
                rec.getRecommendationReason());
    }

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
                .map(this::convertRecommendation)
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
