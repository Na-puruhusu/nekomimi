package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoPose;

/**
 * Where every part of the cat body goes, for walking and for each pose. Only numbers, no game classes,
 * so it can be tried outside the game (tools/preview_pose.py draws these numbers).
 *
 * Model space: 1 unit = 1/16 block, +x is the wearer's left, +y is down, the front is -z, and the ground
 * is y = 24. Every part is positioned by its pivot, and turned about it in the order x, then y, then z.
 * The boxes (relative to the pivot) are those of the normal player model:
 *   head    x -4..4,  y -8..0,  z -2..2 deep 8 (so z -4..4)     body   x -4..4, y 0..12, z -2..2
 *   arms    right x -3..1, left x -1..3, y -2..10, z -2..2       legs   x -2..2, y 0..12, z -2..2
 * The arms are the front legs and the legs are the hind legs.
 *
 * A part is 9 numbers in an array: x, y, z, xRot, yRot, zRot, xScale, yScale, zScale.
 * Scales below 1 shorten a leg, which is how tucked paws are made.
 */
public final class CatPoses {
    public static final int HEAD = 0;
    public static final int BODY = 1;
    public static final int FRONT_RIGHT = 2;
    public static final int FRONT_LEFT = 3;
    public static final int HIND_RIGHT = 4;
    public static final int HIND_LEFT = 5;
    public static final int PARTS = 6;
    public static final int FIELDS = 9;
    public static final int SIZE = PARTS * FIELDS;

    private static final float QUARTER_TURN = (float) (Math.PI / 2.0D);
    /** How far the legs swing when walking at full speed. */
    private static final float GAIT_SWING = 1.3F;

    private CatPoses() {
    }

    /**
     * How the cat is moving, for the walking pose; the sitting-type poses ignore it.
     *
     * @param leap      the boosted jump the cat is in, if any
     * @param crouching the player is crouching (a low, ready-to-spring stance)
     * @param rising    the cat is moving upward (in the air)
     */
    public record Motion(float limbSwing, float limbAmount, boolean airborne, float attackTime,
                         NekoLeap leap, boolean crouching, boolean rising) {
        public static final Motion STILL = new Motion(0.0F, 0.0F, false, 0.0F);

        public Motion(float limbSwing, float limbAmount, boolean airborne, float attackTime) {
            this(limbSwing, limbAmount, airborne, attackTime, NekoLeap.NONE, false, false);
        }
    }

    /**
     * Fills `out` with the placement of all parts.
     *
     * @param time      age of the player in ticks, with the fraction of the tick (for slow breathing and the like)
     * @param headPitch where the player looks up and down, in radians (positive = down)
     * @param headYaw   where the player looks left and right relative to the body, in radians
     */
    public static void compute(NekoPose pose, float[] out, float time, float headPitch, float headYaw, Motion motion) {
        for (int i = 0; i < PARTS; i++) {
            put(out, i, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }
        switch (pose) {
            case SIT -> sit(out, time, headPitch, headYaw);
            case LOAF -> loaf(out, time, headPitch, headYaw, false);
            case SLEEP -> loaf(out, time, headPitch, headYaw, true);
            case STRETCH -> stretch(out, time, headPitch, headYaw);
            case GROOM -> groom(out, time, headPitch, headYaw);
            default -> walk(out, headPitch, headYaw, motion);
        }
    }

    private static void put(float[] out, int part, float x, float y, float z, float rx, float ry, float rz,
                            float sx, float sy, float sz) {
        int o = part * FIELDS;
        out[o] = x;
        out[o + 1] = y;
        out[o + 2] = z;
        out[o + 3] = rx;
        out[o + 4] = ry;
        out[o + 5] = rz;
        out[o + 6] = sx;
        out[o + 7] = sy;
        out[o + 8] = sz;
    }

    private static void put(float[] out, int part, float x, float y, float z, float rx, float ry, float rz) {
        put(out, part, x, y, z, rx, ry, rz, 1.0F, 1.0F, 1.0F);
    }

    private static float clamp(float v, float limit) {
        return Math.max(-limit, Math.min(limit, v));
    }

    // ---- walking: the torso lies flat, a trot where a front leg and the opposite hind leg move together ----

    private static void walk(float[] out, float headPitch, float headYaw, Motion m) {
        if (m.airborne() && m.leap() == NekoLeap.LONG) {
            longLeap(out, headPitch, headYaw, m.rising());
            return;
        }
        if (m.crouching() && !m.airborne()) {
            crouch(out, headPitch, headYaw, m);
            return;
        }
        put(out, BODY, 0.0F, 10.0F, -3.0F, QUARTER_TURN, 0.0F, 0.0F);
        put(out, HEAD, 0.0F, 12.0F, -4.0F, headPitch, headYaw, 0.0F);

        float phase = m.limbSwing() * 0.6662F;
        float amount = Math.min(m.limbAmount(), 1.0F);
        float stride = (float) Math.cos(phase) * GAIT_SWING * amount;
        float frontRight = stride;
        float frontLeft = -stride;
        float hindRight = -stride;
        float hindLeft = stride;
        // In the air the front legs reach forward and the hind legs trail back.
        if (m.airborne()) {
            frontRight = -0.7F;
            frontLeft = -0.7F;
            hindRight = 0.8F;
            hindLeft = 0.8F;
        }
        // Attacking: the right front paw swipes forward.
        if (m.attackTime() > 0.0F) {
            frontRight -= (float) Math.sin(m.attackTime() * Math.PI) * 1.4F;
        }
        put(out, FRONT_RIGHT, -2.0F, 14.0F, -1.0F, frontRight, 0.0F, 0.0F);
        put(out, FRONT_LEFT, 2.0F, 14.0F, -1.0F, frontLeft, 0.0F, 0.0F);
        put(out, HIND_RIGHT, -2.0F, 12.0F, 7.0F, hindRight, 0.0F, 0.0F);
        put(out, HIND_LEFT, 2.0F, 12.0F, 7.0F, hindLeft, 0.0F, 0.0F);
    }

    // ---- crouching: low to the ground, haunches up, ready to spring; it can still creep along ----

    private static void crouch(float[] out, float headPitch, float headYaw, Motion m) {
        put(out, BODY, 0.0F, 14.0F, -3.0F, QUARTER_TURN + 0.12F, 0.0F, 0.0F);
        put(out, HEAD, 0.0F, 16.0F, -4.5F, clamp(headPitch, 0.8F) * 0.8F, clamp(headYaw, 1.0F), 0.0F);

        float stride = (float) Math.cos(m.limbSwing() * 0.6662F) * GAIT_SWING * 0.5F * Math.min(m.limbAmount(), 1.0F);
        // The legs are shortened (bent) so that the paws still reach the ground.
        put(out, FRONT_RIGHT, -2.0F, 17.0F, -1.5F, stride - 0.35F, 0.0F, 0.0F, 1.0F, 0.7F, 1.0F);
        put(out, FRONT_LEFT, 2.0F, 17.0F, -1.5F, -stride - 0.35F, 0.0F, 0.0F, 1.0F, 0.7F, 1.0F);
        put(out, HIND_RIGHT, -2.0F, 15.2F, 7.0F, -stride + 0.5F, 0.0F, 0.0F, 1.0F, 0.8F, 1.0F);
        put(out, HIND_LEFT, 2.0F, 15.2F, 7.0F, stride + 0.5F, 0.0F, 0.0F, 1.0F, 0.8F, 1.0F);
    }

    // ---- long jump: stretched out flat, front legs reaching ahead, hind legs trailing ----

    private static void longLeap(float[] out, float headPitch, float headYaw, boolean rising) {
        // Nose up while rising, nose down while coming down.
        float lean = rising ? -0.2F : 0.15F;
        put(out, BODY, 0.0F, 9.0F, -3.0F, QUARTER_TURN + lean, 0.0F, 0.0F);
        put(out, HEAD, 0.0F, 11.0F, -4.5F, clamp(headPitch, 0.8F) - lean * 0.5F, clamp(headYaw, 1.0F), 0.0F);
        float reach = rising ? -1.75F : -1.45F;
        put(out, FRONT_RIGHT, -2.0F, 12.5F, -1.5F, reach, -0.1F, 0.0F);
        put(out, FRONT_LEFT, 2.0F, 12.5F, -1.5F, reach, 0.1F, 0.0F);
        put(out, HIND_RIGHT, -2.0F, 11.5F, 7.0F, 1.7F, 0.1F, 0.0F);
        put(out, HIND_LEFT, 2.0F, 11.5F, 7.0F, 1.7F, -0.1F, 0.0F);
    }

    // ---- sitting: the torso leans back on the hips, the front legs straight, the hind legs folded forward ----

    private static void sit(float[] out, float time, float headPitch, float headYaw) {
        float breath = (float) Math.sin(time * 0.1D) * 0.12F;
        put(out, BODY, 0.0F, 14.4F + breath, -3.0F, 0.9F, 0.0F, 0.0F);
        // Looking far down would push the chin into the front legs, so that is limited more than looking up.
        put(out, HEAD, 0.0F, 14.8F + breath, -4.2F, Math.max(-0.9F, Math.min(0.5F, headPitch)), clamp(headYaw, 1.2F), 0.0F);
        // The front legs start below the head's chin (their tops must stay hidden when the head tips up).
        put(out, FRONT_RIGHT, -2.5F, 16.0F, -5.5F, 0.05F, 0.0F, 0.0F, 1.0F, 0.8F, 1.0F);
        put(out, FRONT_LEFT, 2.5F, 16.0F, -5.5F, 0.05F, 0.0F, 0.0F, 1.0F, 0.8F, 1.0F);
        put(out, HIND_RIGHT, -3.4F, 21.5F, 6.4F, -1.45F, -0.3F, 0.0F, 1.0F, 0.6F, 1.0F);
        put(out, HIND_LEFT, 3.4F, 21.5F, 6.4F, -1.45F, 0.3F, 0.0F, 1.0F, 0.6F, 1.0F);
    }

    // ---- loaf (paws and legs tucked under the body) and sleeping (the same, curled up, head down) ----

    private static void loaf(float[] out, float time, float headPitch, float headYaw, boolean asleep) {
        float breath = (float) Math.sin(time * (asleep ? 0.09D : 0.1D)) * (asleep ? 0.22F : 0.1F);
        put(out, BODY, 0.0F, 21.8F + breath, -3.0F, QUARTER_TURN, 0.0F, 0.0F);
        if (asleep) {
            // The head rests on the paws, turned to one side and tilted.
            put(out, HEAD, 1.0F, 22.6F + breath, -4.0F, 0.4F, 0.6F, 0.35F);
        } else {
            put(out, HEAD, 0.0F, 23.6F + breath, -4.5F, clamp(headPitch, 0.7F) * 0.6F + 0.1F, clamp(headYaw, 1.0F), 0.0F);
        }
        // Front paws tucked back under the chest, their ends just showing in front.
        put(out, FRONT_RIGHT, -2.5F, 21.8F, -4.0F, 1.5F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        put(out, FRONT_LEFT, 2.5F, 21.8F, -4.0F, 1.5F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        // Hind legs folded along the flanks: the haunches bulge at the sides.
        float curl = asleep ? 0.5F : 0.0F;
        put(out, HIND_RIGHT, -3.5F, 21.8F, 7.0F, -1.5F, curl, 0.0F, 1.0F, 0.5F, 1.0F);
        put(out, HIND_LEFT, 3.5F, 21.8F, 7.0F, -1.5F, -curl, 0.0F, 1.0F, 0.5F, 1.0F);
    }

    // ---- stretching: the chest low and the front legs reaching forward, the hips up ----

    private static void stretch(float[] out, float time, float headPitch, float headYaw) {
        float sway = (float) Math.sin(time * 0.15D) * 0.03F;
        put(out, BODY, 0.0F, 18.5F, -4.0F, QUARTER_TURN + 0.45F + sway, 0.0F, 0.0F);
        put(out, HEAD, 0.0F, 22.5F, -7.5F, 0.35F + clamp(headPitch, 0.6F) * 0.3F, clamp(headYaw, 0.8F), 0.0F);
        put(out, FRONT_RIGHT, -4.2F, 19.5F, -5.5F, -1.2F, -0.12F, 0.0F);
        put(out, FRONT_LEFT, 4.2F, 19.5F, -5.5F, -1.2F, 0.12F, 0.0F);
        put(out, HIND_RIGHT, -3.0F, 12.3F, 6.8F, 0.25F, 0.0F, 0.0F);
        put(out, HIND_LEFT, 3.0F, 12.3F, 6.8F, 0.25F, 0.0F, 0.0F);
    }

    // ---- grooming: sitting, one front paw raised to the mouth, the head bowed to it ----

    private static void groom(float[] out, float time, float headPitch, float headYaw) {
        sit(out, time, 0.0F, 0.0F);
        float lick = (float) Math.sin(time * 0.9D);
        put(out, HEAD, 0.0F, 14.8F, -4.2F, 0.45F + lick * 0.06F, -0.25F, 0.0F);
        put(out, FRONT_RIGHT, -2.5F, 15.5F, -5.5F, -1.95F + lick * 0.18F, 0.0F, 0.0F, 1.0F, 0.7F, 1.0F);
    }
}
