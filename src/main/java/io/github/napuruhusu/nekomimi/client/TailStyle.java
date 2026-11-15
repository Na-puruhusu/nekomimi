package io.github.napuruhusu.nekomimi.client;

/**
 * One pose and way of moving for the tail: the rest angles the springs pull toward, how much the
 * wearer's motion pushes the tail, and the idle sway. {@link TailStyles} lists all of them.
 * Plain data with no Minecraft dependency.
 *
 * Angles are relative joint angles, see {@link TailSimulation}. What "pitch" means depends on the body:
 *  - human body: 0 = hanging straight down, positive = swinging back and then up (about 1.5 = straight back)
 *  - cat body (the torso lies flat): 0 = straight back along the spine, positive = up, negative = down
 */
public final class TailStyle {
    private static final int N = TailSimulation.SEGMENTS;

    /** True when the wearer is drawn as the horizontal cat body. */
    final boolean catFrame;
    final float[] restPitch;
    final float[] restSide;

    // Idle sway: side swing per joint (radians), speed (rad/s), delay per joint, and a small pitch breathing.
    float idleSide = 0.11F;
    float idleRate = 2.1F;
    float idleLag = 0.6F;
    float idlePitch = 0.03F;

    /** Quick flicks of the last joints only (interested). Amplitude and speed. */
    float twitch = 0.0F;
    float twitchRate = 9.0F;

    /** Sign of how forward speed bends the tail: +1 streams it back on the human body, -1 on the cat body. */
    float forwardSign = 1.0F;
    /** How much walking, falling and jumping push the tail up and down (1 = fully). */
    float motionScale = 1.0F;
    /** How much turning and strafing push the tail sideways (1 = fully). */
    float turnScale = 1.0F;
    /** Slow sway in time with the steps, which comes and goes. */
    float walkSway = 0.0F;

    /** Multipliers on the spring stiffness and damping. */
    float stiffness = 1.0F;
    float damping = 1.0F;
    /** Width of the tail (1 = normal). Used to puff the fur up. */
    float thickness = 1.0F;

    /** Limits for the first joint, so the tail does not go through the body. */
    float rootMin = 0.05F;
    float rootMax = 1.5F;

    private TailStyle(boolean catFrame, float[] restPitch, float[] restSide) {
        if (restPitch.length != N || restSide.length != N) {
            throw new IllegalArgumentException("a tail style needs " + N + " angles");
        }
        this.catFrame = catFrame;
        this.restPitch = restPitch;
        this.restSide = restSide;
    }

    static TailStyle human(float[] pitch) {
        return new TailStyle(false, pitch, new float[N]);
    }

    static TailStyle cat(float[] pitch, float[] side) {
        TailStyle style = new TailStyle(true, pitch, side);
        style.forwardSign = -1.0F;
        style.rootMin = -0.5F;
        style.rootMax = 2.2F;
        return style;
    }

    static TailStyle cat(float[] pitch) {
        return cat(pitch, new float[N]);
    }

    TailStyle idle(float side, float rate, float lag, float pitch) {
        this.idleSide = side;
        this.idleRate = rate;
        this.idleLag = lag;
        this.idlePitch = pitch;
        return this;
    }

    TailStyle twitch(float amplitude, float rate) {
        this.twitch = amplitude;
        this.twitchRate = rate;
        return this;
    }

    TailStyle motion(float pitchScale, float sideScale) {
        this.motionScale = pitchScale;
        this.turnScale = sideScale;
        return this;
    }

    TailStyle walk(float amplitude) {
        this.walkSway = amplitude;
        return this;
    }

    TailStyle springs(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
        return this;
    }

    TailStyle thick(float thickness) {
        this.thickness = thickness;
        return this;
    }
}
