package io.github.napuruhusu.nekomimi.state;

/**
 * A boosted jump of the cat: LONG (tap crouch while sprinting) goes far forward. NONE is no leap.
 */
public enum NekoLeap {
    NONE,
    LONG;

    private static final NekoLeap[] VALUES = values();

    /** Looks a leap up by its position, e.g. from a network packet. Out-of-range values give NONE. */
    public static NekoLeap byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}
