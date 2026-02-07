package biz.thonbecker.personal.skatetricks.api;

import lombok.Getter;

@Getter
public enum SupportedTrick {
    OLLIE("Ollie", "Board and rider leave ground completely - tail pops, front foot slides up, board levels in air"),
    FRONTSIDE_180("Frontside 180", "Ollie with 180° rotation where rider's front faces forward direction during spin"),
    BACKSIDE_180("Backside 180", "Ollie with 180° rotation where rider's back faces forward direction during spin"),
    KICKFLIP("Kickflip", "Board flips 360° along its length axis while airborne, front foot flicks off heelside"),
    HEELFLIP("Heelflip", "Board flips 360° along its length axis opposite direction, front foot flicks off toeside"),
    POP_SHUVIT("Pop Shuvit", "Board rotates 180° horizontally beneath the rider without flipping"),
    TREFLIP("Tre Flip", "360 shuvit combined with a kickflip - board spins and flips simultaneously"),
    BOARDSLIDE("Boardslide", "Board slides perpendicular across obstacle with deck on edge"),
    FIFTY_FIFTY("50-50 Grind", "Both trucks grinding along an edge/rail"),
    FIVE_O("5-0 Grind", "Back truck only grinding on edge, nose lifted"),
    NOSEGRIND("Nosegrind", "Front truck only grinding on edge, tail lifted"),
    MANUAL("Manual", "Stationary or slow roll balancing on back wheels with nose CLEARLY LIFTED at steep angle"),
    CRUISING("Cruising", "Normal riding with all four wheels on ground, no trick being performed"),
    DROP_IN("Drop In", "Entering a ramp or bowl from the coping"),
    UNKNOWN("Unknown", "Trick not recognized");

    private final String displayName;
    private final String description;

    SupportedTrick(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
