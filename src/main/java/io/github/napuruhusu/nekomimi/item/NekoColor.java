package io.github.napuruhusu.nekomimi.item;

/** Color variants. The id is used in item ids and texture file names. */
public enum NekoColor {
    WHITE("white"),
    BLACK("black"),
    ORANGE("orange"),
    GRAY("gray");

    private final String id;

    NekoColor(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
