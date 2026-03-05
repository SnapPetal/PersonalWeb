package biz.thonbecker.personal.landscape.platform.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Detailed plant information from USDA Plants API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaPlantDetail(
        @JsonProperty("Symbol") String symbol,
        @JsonProperty("Scientific_Name") String scientificName,
        @JsonProperty("Common_Name") String commonName,
        @JsonProperty("Family_Common_Name") String familyCommonName,
        @JsonProperty("Category") String category,
        @JsonProperty("Native_Status") String nativeStatus,
        @JsonProperty("Duration") String duration,
        @JsonProperty("Growth_Habit") String growthHabit,
        @JsonProperty("Toxicity") String toxicity,
        @JsonProperty("Shape_and_Orientation") String shapeAndOrientation,
        @JsonProperty("Active_Growth_Period") String activeGrowthPeriod,
        @JsonProperty("Flower_Color") String flowerColor,
        @JsonProperty("Bloom_Period") String bloomPeriod,
        @JsonProperty("Mature_Height") Integer matureHeight,
        @JsonProperty("Mature_Width") Integer matureWidth) {}
