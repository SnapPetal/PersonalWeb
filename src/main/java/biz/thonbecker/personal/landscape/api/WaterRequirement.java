package biz.thonbecker.personal.landscape.api;

/**
 * Water requirements for plants.
 */
public enum WaterRequirement {
    /** Drought-tolerant, requires minimal watering once established */
    LOW,

    /** Average water needs, regular watering during dry periods */
    MEDIUM,

    /** High water needs, consistent moisture required */
    HIGH
}
