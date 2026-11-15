package io.github.napuruhusu.nekomimi.state;

/** What the cat feels. It changes how the tail moves in cat mode. NONE is the plain, calm tail. */
public enum NekoEmotion {
    NONE("none"),
    HAPPY("happy"),
    RELAXED("relaxed"),
    ANNOYED("annoyed"),
    SCARED("scared"),
    SURPRISED("surprised"),
    INTERESTED("interested"),
    AFFECTIONATE("affectionate");

    private static final NekoEmotion[] VALUES = values();

    private final String id;

    NekoEmotion(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String translationKey() {
        return "nekomimi.emotion." + id;
    }

    /** Looks an emotion up by its position, e.g. from a network packet. Out-of-range values give NONE. */
    public static NekoEmotion byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}
