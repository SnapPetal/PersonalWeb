package biz.thonbecker.personal.landscape.platform.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Plant data from USDA Plants API search results.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaPlantData(
        @JsonProperty("Symbol") String symbol,
        @JsonProperty("Scientific_Name") String scientificName,
        @JsonProperty("Common_Name") String commonName,
        @JsonProperty("Family_Common_Name") String familyCommonName,
        @JsonProperty("Category") String category,
        @JsonProperty("Native_Status") String nativeStatus,
        @JsonProperty("Duration") String duration,
        @JsonProperty("Growth_Habit") String growthHabit) {}
