package biz.thonbecker.personal.landscape.api;

import java.util.List;

/**
 * AI-generated seasonal analysis of a landscape plan.
 *
 * @param spring Spring season description, care tips, and generated image
 * @param summer Summer season description, care tips, and generated image
 * @param fall Fall season description, care tips, and generated image
 * @param winter Winter season description, care tips, and generated image
 */
public record SeasonalAnalysis(
        SeasonalDescription spring, SeasonalDescription summer, SeasonalDescription fall, SeasonalDescription winter) {

    /**
     * @param description Vivid text description of the landscape in this season
     * @param careTips Season-specific care tips for the planted plants
     * @param imageBase64 Base64-encoded generated image of the landscape in this season (nullable)
     */
    public record SeasonalDescription(String description, List<String> careTips, String imageBase64) {}
}
