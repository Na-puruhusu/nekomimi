package io.github.napuruhusu.nekomimi.client;

/**
 * Spring-chain simulation of a cat tail. Has no Minecraft dependency, so it can be tested on its own.
 *
 * The tail is a chain of {@link #SEGMENTS} joints. Each joint holds two relative angles, measured
 * in the frame of the previous segment (the first one in the frame of the body):
 *   pitch - bend toward the back of the wearer (positive = further back / up)
 *   side  - bend sideways (positive = toward the wearer's right)
 * Each angle is a damped spring that pulls the joint to its target. The target is the rest pose
 * plus a push from how the wearer is moving, plus a gentle idle sway. The springs get softer
 * toward the tip, so the tip lags behind and swings past, like a real tail.
 *
 * Units: seconds, blocks, radians. The wearer's frame is: forward = the way the body faces,
 * left = the wearer's left, up = world up.
 */
public final class TailSimulation {
    public static final int SEGMENTS = 6;
    /** Length of one segment in model units (1/16 block). */
    public static final float SEGMENT_LENGTH = 2.0F;

    /** Relative pitch of each joint at rest. Gives a tail that hangs back and curls up at the tip. */
    private static final float[] REST_PITCH = {0.45F, 0.20F, 0.20F, 0.25F, 0.35F, 0.45F};

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

    // Idle sway and walking sway, per joint.
    private static final float IDLE_SIDE = 0.11F;
    private static final float IDLE_SIDE_RATE = 2.1F;
    private static final float IDLE_LAG = 0.6F;
    private static final float IDLE_PITCH = 0.03F;
    private static final float IDLE_PITCH_RATE = 0.9F;
    private static final float WALK_SIDE = 0.08F;
    private static final float WALK_PHASE_PER_BLOCK = 2.2F;

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
    private static final float ROOT_MIN_PITCH = 0.05F;
    private static final float ROOT_MAX_PITCH = 1.5F;

    private static final float SHARE_SUM = SEGMENTS * (SEGMENTS + 1) / 2.0F;

    private final float[] pitch = new float[SEGMENTS];
    private final float[] side = new float[SEGMENTS];
    private final float[] pitchVel = new float[SEGMENTS];
    private final float[] sideVel = new float[SEGMENTS];
    private final float[] targetPitch = new float[SEGMENTS];
    private final float[] targetSide = new float[SEGMENTS];

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

    public static float restPitch(int joint) {
        return REST_PITCH[joint];
    }

    /** Relative pitch of a joint, including its rest angle. */
    public float getPitch(int joint) {
        return pitch[joint];
    }

    public float getSide(int joint) {
        return side[joint];
    }

    /** Back to the rest pose with no motion, e.g. after a teleport or when first seen. */
    public void reset() {
        for (int i = 0; i < SEGMENTS; i++) {
            pitch[i] = REST_PITCH[i];
            side[i] = 0.0F;
            pitchVel[i] = 0.0F;
            sideVel[i] = 0.0F;
        }
        smoothForward = 0.0F;
        smoothLeft = 0.0F;
        smoothUp = 0.0F;
        smoothYaw = 0.0F;
        pendingTime = 0.0F;
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
        float blend = 1.0F - (float) Math.exp(-dt / INPUT_SMOOTHING);
        smoothForward += (vForward - smoothForward) * blend;
        smoothLeft += (vLeft - smoothLeft) * blend;
        smoothUp += (vUp - smoothUp) * blend;
        smoothYaw += (clamp(yawRate, -MAX_YAW_RATE, MAX_YAW_RATE) - smoothYaw) * blend;

        time += dt;
        float speed = (float) Math.hypot(smoothForward, smoothLeft);
        walkPhase += speed * dt * WALK_PHASE_PER_BLOCK;

        // Moving forward streams the tail backward, falling lifts it, jumping presses it down.
        float pitchDrive = clamp(PITCH_PER_FORWARD_SPEED * smoothForward - PITCH_PER_UP_SPEED * smoothUp,
                -MAX_PITCH_DRIVE, MAX_PITCH_DRIVE);
        // Moving left pushes the tail right; turning right leaves the tail behind to the left.
        float sideDrive = clamp(SIDE_PER_LEFT_SPEED * smoothLeft - SIDE_PER_YAW_RATE * smoothYaw,
                -MAX_SIDE_DRIVE, MAX_SIDE_DRIVE);

        float calm = 1.0F - clamp(speed / 3.0F, 0.0F, 1.0F);
        float idleAmp = IDLE_SIDE * (0.4F + 0.6F * calm);
        float walkAmp = WALK_SIDE * clamp(speed / 4.0F, 0.0F, 1.0F);

        for (int i = 0; i < SEGMENTS; i++) {
            // Joints nearer the tip take a bigger share of the bend.
            float share = (i + 1) / SHARE_SUM;
            targetPitch[i] = REST_PITCH[i] + pitchDrive * share
                    + IDLE_PITCH * (float) Math.sin(time * IDLE_PITCH_RATE - i * 0.5F);
            targetSide[i] = sideDrive * share
                    + idleAmp * (float) Math.sin(time * IDLE_SIDE_RATE - i * IDLE_LAG)
                    + walkAmp * (float) Math.sin(walkPhase - i * 0.5F);
        }

        pendingTime += dt;
        int steps = 0;
        while (pendingTime >= PHYSICS_STEP && steps < MAX_SUBSTEPS) {
            integrate(PHYSICS_STEP);
            pendingTime -= PHYSICS_STEP;
            steps++;
        }
        if (pendingTime >= PHYSICS_STEP) {
            pendingTime = 0.0F;
        }
    }

    private void integrate(float h) {
        for (int i = 0; i < SEGMENTS; i++) {
            float along = i / (float) (SEGMENTS - 1);
            float k = STIFFNESS_ROOT + (STIFFNESS_TIP - STIFFNESS_ROOT) * along;
            float c = 2.0F * DAMPING_RATIO * (float) Math.sqrt(k);

            pitchVel[i] += (-k * (pitch[i] - targetPitch[i]) - c * pitchVel[i]) * h;
            pitch[i] += pitchVel[i] * h;
            sideVel[i] += (-k * (side[i] - targetSide[i]) - c * sideVel[i]) * h;
            side[i] += sideVel[i] * h;

            float minPitch = i == 0 ? ROOT_MIN_PITCH : REST_PITCH[i] - MAX_RELATIVE_PITCH;
            float maxPitch = i == 0 ? ROOT_MAX_PITCH : REST_PITCH[i] + MAX_RELATIVE_PITCH;
            if (pitch[i] < minPitch || pitch[i] > maxPitch) {
                pitch[i] = clamp(pitch[i], minPitch, maxPitch);
                pitchVel[i] = 0.0F;
            }
            if (side[i] < -MAX_SIDE || side[i] > MAX_SIDE) {
                side[i] = clamp(side[i], -MAX_SIDE, MAX_SIDE);
                sideVel[i] = 0.0F;
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
