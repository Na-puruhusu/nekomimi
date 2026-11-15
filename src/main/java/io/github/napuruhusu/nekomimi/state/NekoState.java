package io.github.napuruhusu.nekomimi.state;

/** The mode of one player: normal or cat, and the emotion (only used in cat mode). */
public record NekoState(boolean cat, NekoEmotion emotion) {
    public static final NekoState DEFAULT = new NekoState(false, NekoEmotion.NONE);

    public NekoState {
        if (!cat) {
            emotion = NekoEmotion.NONE;
        }
    }
}
