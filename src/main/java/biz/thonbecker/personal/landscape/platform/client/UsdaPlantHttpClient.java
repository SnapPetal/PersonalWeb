package biz.thonbecker.personal.landscape.platform.client;

import biz.thonbecker.personal.landscape.platform.client.model.UsdaPlantDetail;
import biz.thonbecker.personal.landscape.platform.client.model.UsdaPlantSearchResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/**
 * Declarative HTTP client for USDA Plants Database API.
 *
 * <p>Uses Spring's HTTP Interface abstraction for type-safe API calls.
 */
public interface UsdaPlantHttpClient {

    /**
     * Searches for plants by name and optional filters.
     *
     * @param name Plant name (scientific or common)
     * @param duration Optional duration filter (Annual, Perennial, Biennial)
     * @param growthHabit Optional growth habit filter (Tree, Shrub, Forb/herb, etc.)
     * @param limit Maximum number of results
     * @return Search response with matching plants
     */
    @GetExchange("/search")
    UsdaPlantSearchResponse searchPlants(
            @RequestParam(value = "Sci_Name", required = false) String name,
            @RequestParam(value = "Duration", required = false) String duration,
            @RequestParam(value = "Growth_Habit", required = false) String growthHabit,
            @RequestParam(value = "limit", defaultValue = "50") int limit);

    /**
     * Retrieves detailed information for a specific plant by its USDA symbol.
     *
     * @param usdaSymbol Official USDA plant symbol (e.g., "ACRU")
     * @return Detailed plant information
     */
    @GetExchange("/plant/{symbol}")
    UsdaPlantDetail getPlantDetail(@PathVariable("symbol") String usdaSymbol);
}
