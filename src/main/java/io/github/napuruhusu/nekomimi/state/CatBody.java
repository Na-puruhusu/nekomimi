package io.github.napuruhusu.nekomimi.state;

import net.minecraft.world.entity.Pose;

/**
 * Size of the player in cat mode. The model is NOT scaled: the character keeps exactly the size
 * of the normal player, only the pose changes (all fours). The hitbox and eye height are set to
 * match that pose, see CatPlayerModel for where every part ends up.
 *
 * Measured from the model: the top of the head is 20 model units above the ground (1.17 blocks)
 * and the eyes, at the middle of the head, are 16 units up (0.94 blocks).
 *
 * Only the standing and crouching poses become a cat. Swimming, sleeping, gliding and so on keep
 * the vanilla body, so nothing about those poses has to be redone.
 */
public final class CatBody {
    /** Size of the cat model compared with the normal player model. 1 = the same size. */
    public static final float SCALE = 1.0F;

    public static final float WIDTH = 0.6F;
    public static final float HEIGHT = 1.2F;
    public static final float EYE_HEIGHT = 0.94F;
    public static final float CROUCH_HEIGHT = 1.1F;
    public static final float CROUCH_EYE_HEIGHT = 0.9F;

    private CatBody() {
    }

    public static boolean isCatPose(Pose pose) {
        return pose == Pose.STANDING || pose == Pose.CROUCHING;
    }
}
