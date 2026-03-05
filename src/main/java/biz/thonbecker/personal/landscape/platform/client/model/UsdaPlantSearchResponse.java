package biz.thonbecker.personal.landscape.platform.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from USDA Plants API search endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsdaPlantSearchResponse(@JsonProperty("data") List<UsdaPlantData> data) {}
