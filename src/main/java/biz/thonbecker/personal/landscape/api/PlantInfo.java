package biz.thonbecker.personal.landscape.api;

import java.util.List;

/**
 * Plant information from USDA Plants Database.
 *
 * @param usdaSymbol Official USDA plant symbol (e.g., "ACRU" for Acer rubrum)
 * @param scientificName Botanical/scientific name
 * @param commonName Common name in English
 * @param familyCommonName Plant family common name
 * @param hardinessZones List of USDA zones where plant is hardy
 * @param lightRequirement Sunlight needs
 * @param waterRequirement Water needs
 * @param category Plant type (tree, shrub, perennial, etc.)
 * @param nativeStatus Native, introduced, or invasive status
 * @param matureHeight Expected height at maturity (inches), nullable
 * @param matureWidth Expected width at maturity (inches), nullable
 * @param imageUrl Thumbnail image URL from the plant data provider, nullable
 */
public record PlantInfo(
        String usdaSymbol,
        String scientificName,
        String commonName,
        String familyCommonName,
        List<HardinessZone> hardinessZones,
        LightRequirement lightRequirement,
        WaterRequirement waterRequirement,
        String category,
        String nativeStatus,
        Integer matureHeight,
        Integer matureWidth,
        String imageUrl) {}
