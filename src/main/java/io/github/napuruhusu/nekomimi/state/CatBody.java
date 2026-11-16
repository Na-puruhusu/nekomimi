package io.github.napuruhusu.nekomimi.state;

import net.minecraft.world.entity.Pose;

/**
 * Size of the player in cat mode. The cat body is drawn smaller than the normal player model
 * (SCALE), and the hitbox and eye height match it.
 *
 * The small size is on purpose, because it looks cuter than the full-size body (decided by the
 * owner after trying both). Keep SCALE at 0.6 and build everything else for this size.
 *
 * Only the standing and crouching poses become a cat. Swimming, sleeping, gliding and so on keep
 * the vanilla body, so nothing about those poses has to be redone.
 */
public final class CatBody {
    /** Size of the cat model compared with the normal player model. */
    public static final float SCALE = 0.6F;

    public static final float WIDTH = 0.6F;
    public static final float HEIGHT = 0.7F;
    public static final float EYE_HEIGHT = 0.56F;
    public static final float CROUCH_HEIGHT = 0.6F;
    public static final float CROUCH_EYE_HEIGHT = 0.48F;

    private CatBody() {
    }

    public static boolean isCatPose(Pose pose) {
        return pose == Pose.STANDING || pose == Pose.CROUCHING;
    }
}
