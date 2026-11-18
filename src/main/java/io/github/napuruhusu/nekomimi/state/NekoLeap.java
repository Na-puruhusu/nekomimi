package io.github.napuruhusu.nekomimi.state;

/**
 * What the cat is doing in the air or on a ledge: LONG is the boosted jump (crouch while sprinting, then
 * jump), HANG is hanging from a ledge it caught after that jump, CLIMB is pulling itself up onto the ledge.
 * NONE is none of these.
 */
public enum NekoLeap {
    NONE,
    LONG,
    HANG,
    CLIMB;

    private static final NekoLeap[] VALUES = values();

    /** Looks a leap up by its position, e.g. from a network packet. Out-of-range values give NONE. */
    public static NekoLeap byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}
