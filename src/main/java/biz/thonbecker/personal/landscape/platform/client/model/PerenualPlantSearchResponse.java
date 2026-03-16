package biz.thonbecker.personal.landscape.platform.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from Perenual API species-list endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerenualPlantSearchResponse(
        List<PerenualPlant> data,
        int total,
        @JsonProperty("per_page") int perPage,
        @JsonProperty("current_page") int currentPage) {}
