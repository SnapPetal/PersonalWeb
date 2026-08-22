package biz.thonbecker.personal.tankgame.domain;

import java.util.Arrays;

public enum TankLoadout {
    SCOUT("scout", "Scout", 1),
    STRIKER("striker", "Striker", 2),
    RAIDER("raider", "Raider", 3),
    SIEGE("siege", "Siege", 4),
    SENTINEL("sentinel", "Sentinel", 5),
    HUNTER("hunter", "Hunter", 6),
    JUGGERNAUT("juggernaut", "Juggernaut", 7),
    ARTILLERY("artillery", "Artillery", 8);

    private final String id;
    private final String displayName;
    private final int visualVariant;

    TankLoadout(final String id, final String displayName, final int visualVariant) {
        this.id = id;
        this.displayName = displayName;
        this.visualVariant = visualVariant;
    }

    public static TankLoadout fromId(final String id) {
        return Arrays.stream(values())
                .filter(loadout -> loadout.id.equalsIgnoreCase(id))
                .findFirst()
                .orElse(STRIKER);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getVisualVariant() {
        return visualVariant;
    }
}
