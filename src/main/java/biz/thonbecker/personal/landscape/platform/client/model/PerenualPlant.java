package biz.thonbecker.personal.landscape.platform.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Plant data from Perenual API search results.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerenualPlant(
        int id,
        @JsonProperty("common_name") String commonName,
        @JsonProperty("scientific_name") List<String> scientificName,
        String family,
        @JsonProperty("default_image") DefaultImage defaultImage) {

    /**
     * Image URLs provided by the Perenual API.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DefaultImage(
            String thumbnail,
            @JsonProperty("small_url") String smallUrl,
            @JsonProperty("medium_url") String mediumUrl,
            @JsonProperty("regular_url") String regularUrl,
            @JsonProperty("original_url") String originalUrl) {}
}
