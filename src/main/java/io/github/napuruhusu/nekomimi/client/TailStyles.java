package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoEmotion;

/**
 * Every tail pose. Each array lists the rest angle of the six joints from the root to the tip.
 * The springs always pull toward these, so changing style makes the tail swing smoothly into the new pose.
 */
public final class TailStyles {
    // ---- Normal body ----

    /** Hangs back and curls up at the tip. Sways slowly now and then while walking. */
    public static final TailStyle NORMAL = TailStyle.human(new float[]{0.45F, 0.20F, 0.20F, 0.25F, 0.35F, 0.45F})
            .idle(0.11F, 2.1F, 0.6F, 0.03F)
            .walk(0.12F)
            .ears(0.0F, 0.0F, 0.0F, 0.0F, 0.25F);

    /** Sprinting: stretched out straight behind. Bends sideways when the wearer turns hard. */
    public static final TailStyle SPRINT = TailStyle.human(new float[]{1.35F, 0.03F, 0.03F, 0.03F, 0.03F, 0.03F})
            .idle(0.02F, 3.0F, 0.4F, 0.01F)
            .motion(0.15F, 1.8F)
            .springs(1.3F, 1.0F)
            .ears(-0.35F, -0.35F, 0.15F, 0.15F, 0.1F);

    // ---- Cat body (the torso lies flat; pitch 0 = back along the spine, positive = up) ----

    /** No emotion: carried low and back with the tip turned up, swaying slowly. */
    public static final TailStyle CAT_NEUTRAL = TailStyle.cat(new float[]{0.50F, 0.10F, 0.00F, 0.00F, 0.20F, 0.30F})
            .idle(0.09F, 1.8F, 0.6F, 0.03F)
            .walk(0.05F)
            .ears(0.0F, 0.0F, 0.05F, 0.05F, 0.25F);

    /** Happy: straight up. */
    public static final TailStyle HAPPY = TailStyle.cat(new float[]{1.45F, 0.00F, 0.00F, 0.02F, 0.04F, 0.06F})
            .idle(0.03F, 3.5F, 0.5F, 0.01F)
            .motion(0.3F, 0.5F)
            .springs(1.4F, 1.0F)
            .fur(1.0F, 1.0F, 0.0F, 0.2F)
            .ears(0.12F, 0.12F, 0.0F, 0.0F, 0.2F);

    /** Relaxed: let down beside the body, loose. */
    public static final TailStyle RELAXED = TailStyle.cat(
                    new float[]{0.05F, -0.25F, -0.25F, -0.20F, -0.10F, 0.00F},
                    new float[]{0.10F, 0.12F, 0.12F, 0.12F, 0.10F, 0.10F})
            .idle(0.04F, 1.1F, 0.5F, 0.01F)
            .motion(0.3F, 0.5F)
            .springs(0.6F, 1.0F)
            .fur(0.75F, 1.5F, 0.0F, 0.45F)
            .ears(-0.15F, -0.15F, 0.3F, 0.3F, 0.1F);

    /** Annoyed: held low and lashed hard from side to side. */
    public static final TailStyle ANNOYED = TailStyle.cat(new float[]{0.20F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F})
            .idle(0.22F, 7.5F, 0.25F, 0.02F)
            .motion(0.3F, 0.3F)
            .springs(1.0F, 1.0F)
            .fur(1.1F, 1.0F, 0.0F, 0.2F)
            .ears(-0.25F, -0.25F, 1.0F, 1.0F, 0.5F);

    /** Scared: curled under the belly between the hind legs, trembling. */
    public static final TailStyle SCARED = TailStyle.cat(new float[]{0.00F, -0.60F, -0.65F, -0.65F, -0.55F, -0.50F})
            .idle(0.03F, 26.0F, 0.3F, 0.02F)
            .motion(0.3F, 0.3F)
            .springs(1.2F, 1.0F)
            .fur(1.25F, 1.0F, 0.2F, 0.15F)
            .ears(-1.1F, -1.1F, 0.45F, 0.45F, 0.0F);

    /** Startled or threatening: arched up and over the back with the fur puffed out thick. */
    public static final TailStyle SURPRISED = TailStyle.cat(new float[]{1.00F, 0.45F, 0.35F, 0.30F, 0.30F, 0.30F})
            .idle(0.02F, 12.0F, 0.3F, 0.02F)
            .motion(0.3F, 0.3F)
            .springs(1.5F, 1.0F)
            .thick(1.4F)
            .fur(1.6F, 0.3F, 0.05F, 0.0F)
            .ears(0.35F, 0.35F, 0.12F, 0.12F, 0.0F);

    /** Interested: held low and still, only the tip flicks. */
    public static final TailStyle INTERESTED = TailStyle.cat(new float[]{0.00F, -0.05F, -0.05F, -0.05F, 0.00F, 0.00F})
            .idle(0.01F, 2.0F, 0.4F, 0.01F)
            .twitch(0.35F, 9.0F)
            .motion(0.3F, 0.3F)
            .springs(1.3F, 1.0F)
            .ears(0.5F, 0.2F, 0.05F, 0.0F, 1.0F);

    /** Affectionate: curled round to the side, as if wrapping around something. */
    public static final TailStyle AFFECTIONATE = TailStyle.cat(
                    new float[]{0.10F, -0.05F, -0.05F, -0.05F, -0.05F, -0.05F},
                    new float[]{0.45F, 0.50F, 0.50F, 0.50F, 0.50F, 0.50F})
            .idle(0.03F, 1.2F, 0.5F, 0.01F)
            .motion(0.3F, 0.5F)
            .springs(0.8F, 1.0F)
            .fur(0.9F, 1.3F, 0.0F, 0.3F)
            .ears(-0.3F, -0.3F, 0.3F, 0.3F, 0.2F);

    private TailStyles() {
    }

    public static TailStyle forEmotion(NekoEmotion emotion) {
        return switch (emotion) {
            case NONE -> CAT_NEUTRAL;
            case HAPPY -> HAPPY;
            case RELAXED -> RELAXED;
            case ANNOYED -> ANNOYED;
            case SCARED -> SCARED;
            case SURPRISED -> SURPRISED;
            case INTERESTED -> INTERESTED;
            case AFFECTIONATE -> AFFECTIONATE;
        };
    }
}
