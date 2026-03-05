package biz.thonbecker.personal.landscape.api;

/**
 * Sunlight requirements for plants.
 */
public enum LightRequirement {
    /** Requires 6+ hours of direct sunlight daily */
    FULL_SUN,

    /** Requires 3-6 hours of sunlight, or dappled shade */
    PARTIAL_SHADE,

    /** Requires less than 3 hours of direct sunlight */
    FULL_SHADE
}
