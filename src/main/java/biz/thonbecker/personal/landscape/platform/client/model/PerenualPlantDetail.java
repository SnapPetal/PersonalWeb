package biz.thonbecker.personal.landscape.platform.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Detailed plant data from Perenual API species/details endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerenualPlantDetail(
        int id,
        @JsonProperty("common_name") String commonName,
        @JsonProperty("scientific_name") List<String> scientificName,
        String family,
        List<String> sunlight,
        String watering,
        Hardiness hardiness,
        String type,
        @JsonProperty("default_image") PerenualPlant.DefaultImage defaultImage) {

    /**
     * Hardiness zone range from the Perenual API.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hardiness(String min, String max) {}
}
