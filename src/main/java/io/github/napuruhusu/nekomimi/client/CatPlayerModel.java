package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * The player model posed on all fours. Nothing here comes from the vanilla crawling or swimming
 * poses: setupAnim does not call the normal humanoid animation at all, it places every part itself.
 *
 * Model space: 1 unit = 1/16 block, +y is down, the front of the model is -z, the ground is y = 24.
 * The pieces are placed like this (the whole model is then drawn at CatBody.SCALE):
 *
 *   torso      lies flat, turned a quarter turn about its front end, so it runs from z = -3 (shoulders)
 *              to z = 9 (hips) and its 4 unit thickness is y = 8..12
 *   head       at the front, at about the height of the back, looking where the player looks
 *   front legs the arms, hanging from the shoulders (y = 12 down to the ground)
 *   hind legs  the legs, hanging from the hips
 *
 * Everything is centered on the player's position, so the model reaches 9 units in front of it and behind it.
 */
public class CatPlayerModel extends PlayerModel<AbstractClientPlayer> {
    private static final float BODY_Y = 10.0F;
    private static final float BODY_Z = -3.0F;
    private static final float HEAD_Y = 12.0F;
    private static final float HEAD_Z = -4.0F;
    private static final float FRONT_X = 2.0F;
    private static final float FRONT_Y = 14.0F;
    private static final float FRONT_Z = -1.0F;
    private static final float HIND_X = 2.0F;
    private static final float HIND_Y = 12.0F;
    private static final float HIND_Z = 7.0F;

    private static final float QUARTER_TURN = (float) (Math.PI / 2.0D);
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);
    /** How far the legs swing when walking at full speed. */
    private static final float GAIT_SWING = 1.3F;

    public CatPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Torso: lying flat with the back up.
        this.body.setPos(0.0F, BODY_Y, BODY_Z);
        this.body.xRot = QUARTER_TURN;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;

        // Head: looks where the player looks.
        this.head.setPos(0.0F, HEAD_Y, HEAD_Z);
        this.head.xRot = headPitch * DEG_TO_RAD;
        this.head.yRot = netHeadYaw * DEG_TO_RAD;
        this.head.zRot = 0.0F;

        // Legs: a trot, where the front leg and the hind leg on opposite sides move together.
        float phase = limbSwing * 0.6662F;
        float amount = Math.min(limbSwingAmount, 1.0F);
        float stride = Mth.cos(phase) * GAIT_SWING * amount;
        float frontRight = stride;
        float frontLeft = -stride;
        float hindRight = -stride;
        float hindLeft = stride;

        // In the air the front legs reach forward and the hind legs trail back.
        if (!player.onGround() && !player.isInWater()) {
            frontRight = -0.7F;
            frontLeft = -0.7F;
            hindRight = 0.8F;
            hindLeft = 0.8F;
        }
        // Attacking: the right front paw swipes forward.
        if (this.attackTime > 0.0F) {
            frontRight -= Mth.sin(this.attackTime * (float) Math.PI) * 1.4F;
        }

        placeLeg(this.rightArm, -FRONT_X, FRONT_Y, FRONT_Z, frontRight);
        placeLeg(this.leftArm, FRONT_X, FRONT_Y, FRONT_Z, frontLeft);
        placeLeg(this.rightLeg, -HIND_X, HIND_Y, HIND_Z, hindRight);
        placeLeg(this.leftLeg, HIND_X, HIND_Y, HIND_Z, hindLeft);

        // The skin's second layer follows the part under it.
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }

    private static void placeLeg(ModelPart leg, float x, float y, float z, float swing) {
        leg.setPos(x, y, z);
        leg.xRot = swing;
        leg.yRot = 0.0F;
        leg.zRot = 0.0F;
    }
}
