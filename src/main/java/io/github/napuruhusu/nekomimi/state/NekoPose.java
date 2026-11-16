package io.github.napuruhusu.nekomimi.state;

/**
 * A pose the cat holds while standing still (cat mode only). Walking, jumping or being hurt ends it,
 * and STRETCH ends by itself after a moment. NONE is the normal four-legged stance.
 */
public enum NekoPose {
    NONE("none"),
    SIT("sit"),
    LOAF("loaf"),
    STRETCH("stretch"),
    GROOM("groom"),
    SLEEP("sleep");

    private static final NekoPose[] VALUES = values();

    private final String id;

    NekoPose(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String translationKey() {
        return "nekomimi.pose." + id;
    }

    /** Looks a pose up by its position, e.g. from a network packet. Out-of-range values give NONE. */
    public static NekoPose byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}
