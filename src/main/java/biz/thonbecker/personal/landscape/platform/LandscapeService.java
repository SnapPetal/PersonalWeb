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
        final var uploadResult = imageStorageService.store(imageData, "image/jpeg", userId);

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

    @Transactional(readOnly = true)
    public List<PlantInfo> getRecommendations(final Long planId) {
        log.debug("Fetching plant recommendations for plan {}", planId);

        final var plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
        final var zone = HardinessZone.valueOf(plan.getHardinessZone());

        return plantApiService.getPlantsByZone(zone);
    }

    @Transactional
    public void addPlantPlacement(
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

        placementRepository.save(placement);

        log.info("Successfully added plant placement for plan {}", planId);
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

        final var plantNames = plan.getPlacements().stream()
                .map(p -> Objects.nonNull(p.getCommonName()) ? p.getCommonName() : p.getPlantName())
                .toList();

        // Fetch the original image from S3
        final var getRequest = GetObjectRequest.builder()
                .bucket(imageStorageService.getBucketName())
                .key(plan.getImageS3Key())
                .build();

        try (final ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest)) {
            final var imageData = s3Object.readAllBytes();

            // Get text descriptions from Claude
            final var textAnalysis = aiService.analyzeSeasons(
                    imageData, HardinessZone.valueOf(plan.getHardinessZone()), plantDescriptions);

            // Generate seasonal images with Nova Canvas
            final var springImage = imageGenerationService.generateSeasonalImage(imageData, "spring", plantNames);
            final var summerImage = imageGenerationService.generateSeasonalImage(imageData, "summer", plantNames);
            final var fallImage = imageGenerationService.generateSeasonalImage(imageData, "fall", plantNames);
            final var winterImage = imageGenerationService.generateSeasonalImage(imageData, "winter", plantNames);

            // Merge text descriptions with generated images
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

    public String getPlantImageUrl(final String usdaSymbol) {
        return plantImageService.getPlantImageUrl(usdaSymbol);
    }

    private SeasonalAnalysis.SeasonalDescription mergeWithImage(
            final SeasonalAnalysis.SeasonalDescription description, final String imageBase64) {
        if (description == null) {
            return null;
        }
        return new SeasonalAnalysis.SeasonalDescription(description.description(), description.careTips(), imageBase64);
    }

    private static PlantInfo convertRecommendation(final RecommendedPlantEntity rec) {
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
                null);
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
                .map(LandscapeService::convertRecommendation)
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
