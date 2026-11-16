package io.github.napuruhusu.nekomimi.client;

/**
 * Spring-chain simulation of a cat tail. Has no Minecraft dependency, so it can be tested on its own.
 *
 * The tail is a chain of {@link #SEGMENTS} joints. Each joint holds two relative angles, measured
 * in the frame of the previous segment (the first one in the frame of the body):
 *   pitch - bend of the tail (see {@link TailStyle} for what it means on the human and the cat body)
 *   side  - bend sideways (positive = toward the wearer's right)
 * Each angle is a damped spring that pulls the joint to its target. The target is the rest pose of
 * the current {@link TailStyle}, plus a push from how the wearer is moving, plus idle sway. The
 * springs get softer toward the tip, so the tip lags behind and swings past, like a real tail.
 *
 * Units: seconds, blocks, radians. The wearer's frame is: forward = the way the body faces,
 * left = the wearer's left, up = world up.
 */
public final class TailSimulation {
    public static final int SEGMENTS = 6;
    /** Length of one segment in model units (1/16 block). */
    public static final float SEGMENT_LENGTH = 2.0F;

    private static final float STIFFNESS_ROOT = 70.0F;
    private static final float STIFFNESS_TIP = 25.0F;
    private static final float DAMPING_RATIO = 0.25F;

    // How strongly motion bends the whole chain (radians per unit, summed over all joints).
    private static final float PITCH_PER_FORWARD_SPEED = 0.12F;
    private static final float PITCH_PER_UP_SPEED = 0.08F;
    private static final float SIDE_PER_LEFT_SPEED = 0.22F;
    private static final float SIDE_PER_YAW_RATE = 0.09F;
    private static final float MAX_PITCH_DRIVE = 1.2F;
    private static final float MAX_SIDE_DRIVE = 1.0F;
    private static final float MAX_YAW_RATE = 15.0F;

    private static final float IDLE_PITCH_RATE = 0.9F;
    /** Radians of walking sway phase per block walked, so the sway keeps time with the steps. */
    private static final float WALK_PHASE_PER_BLOCK = 4.0F * 0.6662F;
    /** How fast the width of the tail follows the style. */
    private static final float THICKNESS_SMOOTHING = 0.15F;

    // Fur: it is blown about by the wearer's motion and sinks under its own weight, on a soft spring of its own.
    /** Model units the outermost fur layer is pushed per block/second of speed. */
    private static final float FUR_PUSH_PER_SPEED = 0.10F;
    private static final float FUR_MAX_PUSH = 0.6F;
    private static final float FUR_STIFFNESS = 55.0F;
    private static final float FUR_DAMPING_RATIO = 0.3F;
    private static final float FUR_SHIVER_RATE = 38.0F;
    private static final float FUR_LENGTH_SMOOTHING = 0.15F;

    private static final float INPUT_SMOOTHING = 0.05F;
    /**
     * The springs are always integrated with this fixed step, whatever the frame rate, so the motion
     * looks the same at 30 and at 240 fps. Left-over time is carried to the next frame. If the game
     * stutters badly, at most MAX_SUBSTEPS are run per frame and the rest is dropped.
     */
    private static final float PHYSICS_STEP = 1.0F / 120.0F;
    private static final int MAX_SUBSTEPS = 16;

    private static final float MAX_RELATIVE_PITCH = 0.8F;
    private static final float MAX_SIDE = 0.8F;

    private static final float SHARE_SUM = SEGMENTS * (SEGMENTS + 1) / 2.0F;

    private final float[] pitch = new float[SEGMENTS];
    private final float[] side = new float[SEGMENTS];
    private final float[] pitchVel = new float[SEGMENTS];
    private final float[] sideVel = new float[SEGMENTS];
    private final float[] targetPitch = new float[SEGMENTS];
    private final float[] targetSide = new float[SEGMENTS];

    private TailStyle style = TailStyles.NORMAL;
    private float thickness = 1.0F;
    private float furLength = 1.0F;
    /** Fur offset of the outermost layer, in the human model frame: x = the wearer's left, y = down, z = back. */
    private final float[] furOffset = new float[3];
    private final float[] furVel = new float[3];
    private final float[] furTarget = new float[3];
    private float furShiverX;
    private float furShiverY;

    // Ears: angle and speed of each ear (0 = left, 1 = right), the targets, and when each flicks next.
    private static final float EAR_STIFFNESS = 140.0F;
    private static final float EAR_DAMPING_RATIO = 0.4F;
    /** Speed given to an ear for a flick, radians per second. */
    private static final float EAR_FLICK_SPEED = 7.0F;
    private final float[] earPitch = new float[2];
    private final float[] earOut = new float[2];
    private final float[] earPitchVel = new float[2];
    private final float[] earOutVel = new float[2];
    private final float[] earPitchTarget = new float[2];
    private final float[] earOutTarget = new float[2];
    private final float[] earShiver = new float[2];
    private final float[] nextFlick = new float[2];
    private final java.util.Random random = new java.util.Random();

    private float smoothForward;
    private float smoothLeft;
    private float smoothUp;
    private float smoothYaw;
    private float time;
    private float walkPhase;
    private float pendingTime;

    public TailSimulation() {
        reset();
    }

    /** Rest pitch of a joint on the normal body. */
    public static float restPitch(int joint) {
        return TailStyles.NORMAL.restPitch[joint];
    }

    /** Relative pitch of a joint, including its rest angle. */
    public float getPitch(int joint) {
        return pitch[joint];
    }

    public float getSide(int joint) {
        return side[joint];
    }

    /** Width of the tail, 1 = normal. */
    public float getThickness() {
        return thickness;
    }

    /** Fur length compared with normal, 1 = normal. */
    public float getFurLength() {
        return furLength;
    }

    /**
     * How far the outermost fur layer is displaced, in model units, along axis 0 = the wearer's left,
     * 1 = down, 2 = back (the frame of an upright body). Inner layers move less. Includes trembling.
     */
    public float getFurOffset(int axis) {
        if (axis == 0) {
            return furOffset[0] + furShiverX;
        }
        return axis == 1 ? furOffset[1] + furShiverY : furOffset[2];
    }

    /** Angle of an ear (0 = left, 1 = right): positive = forward, negative = back, in radians. */
    public float getEarPitch(int side) {
        return earPitch[side] + earShiver[side];
    }

    /** How far an ear's tip leans away from the head (0 = left, 1 = right), in radians; negative = toward it. */
    public float getEarOut(int side) {
        return earOut[side];
    }

    public TailStyle getStyle() {
        return style;
    }

    /**
     * Changes the pose and way of moving. The tail swings smoothly into the new pose. Switching
     * between the human and the cat body jumps straight to the new rest pose, because the angles
     * mean something else there.
     */
    public void setStyle(TailStyle newStyle) {
        if (newStyle == style) {
            return;
        }
        boolean frameChanged = newStyle.catFrame != style.catFrame;
        style = newStyle;
        if (frameChanged) {
            snapToRest();
            thickness = style.thickness;
            furLength = style.furLength;
        }
    }

    /** Back to the rest pose with no motion, e.g. after a teleport or when first seen. */
    public void reset() {
        snapToRest();
        smoothForward = 0.0F;
        smoothLeft = 0.0F;
        smoothUp = 0.0F;
        smoothYaw = 0.0F;
        pendingTime = 0.0F;
        thickness = style.thickness;
        furLength = style.furLength;
        for (int a = 0; a < 3; a++) {
            furOffset[a] = 0.0F;
            furVel[a] = 0.0F;
        }
        furShiverX = 0.0F;
        furShiverY = 0.0F;
        for (int e = 0; e < 2; e++) {
            earPitch[e] = style.earPitch[e];
            earOut[e] = style.earOut[e];
            earPitchVel[e] = 0.0F;
            earOutVel[e] = 0.0F;
            nextFlick[e] = time + flickDelay(style.earTwitchRate);
        }
    }

    /** Time until the next flick of an ear: random, on average 1 / rate seconds. */
    private float flickDelay(float rate) {
        return rate > 0.0F ? (0.4F + 1.2F * random.nextFloat()) / rate : 1.0F;
    }

    private void snapToRest() {
        for (int i = 0; i < SEGMENTS; i++) {
            pitch[i] = style.restPitch[i];
            side[i] = style.restSide[i];
            pitchVel[i] = 0.0F;
            sideVel[i] = 0.0F;
        }
    }

    /**
     * Advances the simulation.
     *
     * @param dt         time step in seconds
     * @param vForward   speed along the facing direction, in blocks per second
     * @param vLeft      speed toward the wearer's left
     * @param vUp        vertical speed, positive = upward
     * @param yawRate    turning speed in radians per second, positive = turning right (clockwise from above)
     */
    public void step(float dt, float vForward, float vLeft, float vUp, float yawRate) {
        if (!(dt > 0.0F) || !Float.isFinite(vForward + vLeft + vUp + yawRate)) {
            return;
        }
        TailStyle s = style;
        float blend = 1.0F - (float) Math.exp(-dt / INPUT_SMOOTHING);
        smoothForward += (vForward - smoothForward) * blend;
        smoothLeft += (vLeft - smoothLeft) * blend;
        smoothUp += (vUp - smoothUp) * blend;
        smoothYaw += (clamp(yawRate, -MAX_YAW_RATE, MAX_YAW_RATE) - smoothYaw) * blend;

        time += dt;
        float speed = (float) Math.hypot(smoothForward, smoothLeft);
        walkPhase += speed * dt * WALK_PHASE_PER_BLOCK;
        thickness += (s.thickness - thickness) * (1.0F - (float) Math.exp(-dt / THICKNESS_SMOOTHING));
        furLength += (s.furLength - furLength) * (1.0F - (float) Math.exp(-dt / FUR_LENGTH_SMOOTHING));

        // Fur streams away from the direction of travel and sinks down; shivering is added on top.
        furTarget[0] = clamp(-smoothLeft * FUR_PUSH_PER_SPEED, -FUR_MAX_PUSH, FUR_MAX_PUSH) * s.furSway;
        furTarget[1] = clamp(smoothUp * FUR_PUSH_PER_SPEED, -FUR_MAX_PUSH, FUR_MAX_PUSH) * s.furSway + s.furDroop;
        furTarget[2] = clamp(smoothForward * FUR_PUSH_PER_SPEED, -FUR_MAX_PUSH, FUR_MAX_PUSH) * s.furSway;
        furShiverX = s.furShiver * (float) Math.sin(time * FUR_SHIVER_RATE);
        furShiverY = s.furShiver * 0.7F * (float) Math.sin(time * FUR_SHIVER_RATE * 1.27F + 1.0F);

        // Ears: pointed where the mood puts them, laid back when falling, trembling when scared,
        // and flicking now and then (one ear at a time).
        float fall = clamp(smoothUp * 0.05F, -0.5F, 0.0F);
        for (int e = 0; e < 2; e++) {
            earPitchTarget[e] = s.earPitch[e] + fall;
            // Trembling is added after the spring, which would smooth such a fast shake away.
            earShiver[e] = s.furShiver * 0.8F * (float) Math.sin(time * FUR_SHIVER_RATE * (1.1F + 0.15F * e));
            earOutTarget[e] = s.earOut[e];
            if (s.earTwitchRate > 0.0F && time >= nextFlick[e]) {
                earPitchVel[e] -= EAR_FLICK_SPEED;
                earOutVel[e] += (random.nextBoolean() ? 1.0F : -1.0F) * EAR_FLICK_SPEED * 0.3F;
                nextFlick[e] = time + flickDelay(s.earTwitchRate);
            }
        }

        // Moving forward streams the tail backward, falling lifts it, jumping presses it down.
        float pitchDrive = clamp(PITCH_PER_FORWARD_SPEED * s.forwardSign * smoothForward
                        - PITCH_PER_UP_SPEED * smoothUp, -MAX_PITCH_DRIVE, MAX_PITCH_DRIVE) * s.motionScale;
        // Moving left pushes the tail right; turning right leaves the tail behind to the left.
        float sideDrive = clamp(SIDE_PER_LEFT_SPEED * smoothLeft - SIDE_PER_YAW_RATE * smoothYaw,
                -MAX_SIDE_DRIVE, MAX_SIDE_DRIVE) * s.turnScale;

        // On the human body the idle sway fades while moving. A cat pose keeps its motion.
        float calm = 1.0F - clamp(speed / 3.0F, 0.0F, 1.0F);
        float idleAmp = s.catFrame ? s.idleSide : s.idleSide * (0.4F + 0.6F * calm);
        // Walking sway comes and goes: sometimes the tail sways with the steps, sometimes it does not.
        float gate = 0.5F + 0.5F * (float) Math.sin(time * 0.45F);
        float walkAmp = s.walkSway * clamp(speed / 4.0F, 0.0F, 1.0F) * gate * gate;
        float flick = s.twitch * (0.6F + 0.4F * (float) Math.sin(time * 1.9F))
                * (float) Math.sin(time * s.twitchRate);

        for (int i = 0; i < SEGMENTS; i++) {
            // Joints nearer the tip take a bigger share of the bend.
            float share = (i + 1) / SHARE_SUM;
            float tipWeight = clamp((i - 2) / 3.0F, 0.0F, 1.0F);
            targetPitch[i] = s.restPitch[i] + pitchDrive * share
                    + s.idlePitch * (float) Math.sin(time * IDLE_PITCH_RATE - i * 0.5F);
            targetSide[i] = s.restSide[i] + sideDrive * share
                    + idleAmp * (float) Math.sin(time * s.idleRate - i * s.idleLag)
                    + walkAmp * (float) Math.sin(walkPhase - i * 0.5F)
                    + flick * tipWeight;
        }

        pendingTime += dt;
        int steps = 0;
        while (pendingTime >= PHYSICS_STEP && steps < MAX_SUBSTEPS) {
            integrate(PHYSICS_STEP, s);
            pendingTime -= PHYSICS_STEP;
            steps++;
        }
        if (pendingTime >= PHYSICS_STEP) {
            pendingTime = 0.0F;
        }
    }

    private void integrate(float h, TailStyle s) {
        float ec = 2.0F * EAR_DAMPING_RATIO * (float) Math.sqrt(EAR_STIFFNESS);
        for (int e = 0; e < 2; e++) {
            earPitchVel[e] += (-EAR_STIFFNESS * (earPitch[e] - earPitchTarget[e]) - ec * earPitchVel[e]) * h;
            earPitch[e] += earPitchVel[e] * h;
            earOutVel[e] += (-EAR_STIFFNESS * (earOut[e] - earOutTarget[e]) - ec * earOutVel[e]) * h;
            earOut[e] += earOutVel[e] * h;
        }
        float fc = 2.0F * FUR_DAMPING_RATIO * (float) Math.sqrt(FUR_STIFFNESS);
        for (int a = 0; a < 3; a++) {
            furVel[a] += (-FUR_STIFFNESS * (furOffset[a] - furTarget[a]) - fc * furVel[a]) * h;
            furOffset[a] += furVel[a] * h;
        }
        for (int i = 0; i < SEGMENTS; i++) {
            float along = i / (float) (SEGMENTS - 1);
            float k = (STIFFNESS_ROOT + (STIFFNESS_TIP - STIFFNESS_ROOT) * along) * s.stiffness;
            float c = 2.0F * DAMPING_RATIO * s.damping * (float) Math.sqrt(k);

            pitchVel[i] += (-k * (pitch[i] - targetPitch[i]) - c * pitchVel[i]) * h;
            pitch[i] += pitchVel[i] * h;
            sideVel[i] += (-k * (side[i] - targetSide[i]) - c * sideVel[i]) * h;
            side[i] += sideVel[i] * h;

            float minPitch = i == 0 ? s.rootMin : s.restPitch[i] - MAX_RELATIVE_PITCH;
            float maxPitch = i == 0 ? s.rootMax : s.restPitch[i] + MAX_RELATIVE_PITCH;
            if (pitch[i] < minPitch || pitch[i] > maxPitch) {
                pitch[i] = clamp(pitch[i], minPitch, maxPitch);
                pitchVel[i] = 0.0F;
            }
            float minSide = s.restSide[i] - MAX_SIDE;
            float maxSide = s.restSide[i] + MAX_SIDE;
            if (side[i] < minSide || side[i] > maxSide) {
                side[i] = clamp(side[i], minSide, maxSide);
                sideVel[i] = 0.0F;
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
