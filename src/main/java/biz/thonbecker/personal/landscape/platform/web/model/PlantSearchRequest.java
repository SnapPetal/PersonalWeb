package biz.thonbecker.personal.landscape.platform.web.model;

import biz.thonbecker.personal.landscape.api.HardinessZone;
import biz.thonbecker.personal.landscape.api.LightRequirement;
import biz.thonbecker.personal.landscape.api.WaterRequirement;

/**
 * Request DTO for plant search.
 *
 * @param query Search query
 * @param zone Hardiness zone
 * @param lightRequirement Optional light filter
 * @param waterRequirement Optional water filter
 */
public record PlantSearchRequest(
        String query, HardinessZone zone, LightRequirement lightRequirement, WaterRequirement waterRequirement) {}
