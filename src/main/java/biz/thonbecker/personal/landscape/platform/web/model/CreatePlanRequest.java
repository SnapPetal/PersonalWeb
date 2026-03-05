package biz.thonbecker.personal.landscape.platform.web.model;

import biz.thonbecker.personal.landscape.api.HardinessZone;

/**
 * Request DTO for creating a new landscape plan.
 *
 * @param name Plan name
 * @param description Optional description
 * @param hardinessZone USDA hardiness zone
 * @param zipCode Optional zip code
 */
public record CreatePlanRequest(String name, String description, HardinessZone hardinessZone, String zipCode) {}
