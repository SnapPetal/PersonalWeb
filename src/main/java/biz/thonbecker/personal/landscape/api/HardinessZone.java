package biz.thonbecker.personal.landscape.api;

/**
 * USDA Plant Hardiness Zones.
 *
 * <p>These zones are based on average annual minimum winter temperature, divided into 10-degree F
 * zones. Each zone is further divided into a and b segments (not modeled here for simplicity).
 *
 * @see <a href="https://planthardiness.ars.usda.gov/">USDA Plant Hardiness Zone Map</a>
 */
public enum HardinessZone {
    /** Zone 1: -60°F to -50°F */
    ZONE_1,

    /** Zone 2: -50°F to -40°F */
    ZONE_2,

    /** Zone 3: -40°F to -30°F */
    ZONE_3,

    /** Zone 4: -30°F to -20°F */
    ZONE_4,

    /** Zone 5: -20°F to -10°F */
    ZONE_5,

    /** Zone 6: -10°F to 0°F */
    ZONE_6,

    /** Zone 7: 0°F to 10°F */
    ZONE_7,

    /** Zone 8: 10°F to 20°F */
    ZONE_8,

    /** Zone 9: 20°F to 30°F */
    ZONE_9,

    /** Zone 10: 30°F to 40°F */
    ZONE_10,

    /** Zone 11: 40°F to 50°F */
    ZONE_11,

    /** Zone 12: 50°F to 60°F */
    ZONE_12,

    /** Zone 13: 60°F and above */
    ZONE_13;

    /**
     * Returns the numeric zone value (1-13).
     *
     * @return the zone number
     */
    public int getZoneNumber() {
        return ordinal() + 1;
    }
}
