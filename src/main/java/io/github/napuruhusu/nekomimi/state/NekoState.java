package io.github.napuruhusu.nekomimi.state;

/** The mode of one player: normal or cat, and the emotion and pose (both only used in cat mode). */
public record NekoState(boolean cat, NekoEmotion emotion, NekoPose pose) {
    public static final NekoState DEFAULT = new NekoState(false, NekoEmotion.NONE, NekoPose.NONE);

    public NekoState {
        if (!cat) {
            emotion = NekoEmotion.NONE;
            pose = NekoPose.NONE;
        }
    }
}
