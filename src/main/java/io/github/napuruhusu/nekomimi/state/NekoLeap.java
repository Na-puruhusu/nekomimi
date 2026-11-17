package io.github.napuruhusu.nekomimi.state;

/**
 * A boosted jump of the cat: LONG (sprint, crouch, let go quickly, jump) goes far forward, HIGH (the
 * same from a standstill or a walk) goes high up. NONE is an ordinary jump.
 */
public enum NekoLeap {
    NONE,
    LONG,
    HIGH;

    private static final NekoLeap[] VALUES = values();

    /** Looks a leap up by its position, e.g. from a network packet. Out-of-range values give NONE. */
    public static NekoLeap byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}
