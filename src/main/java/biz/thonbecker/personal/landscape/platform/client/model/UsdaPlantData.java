package biz.thonbecker.personal.landscape.platform.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Plant data from USDA Plants Services API search results.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaPlantData(
        @JsonProperty("Symbol") String symbol,
        @JsonProperty("ScientificNameWithoutAuthor") String scientificName,
        @JsonProperty("CommonName") String commonName,
        @JsonProperty("FamilyName") String familyCommonName,
        @JsonProperty("NumImages") int numImages,
        @JsonProperty("ProfileImageFilename") String profileImageFilename) {}
