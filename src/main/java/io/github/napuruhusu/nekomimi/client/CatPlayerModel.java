package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoPose;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * The player model posed on all fours. Nothing here comes from the vanilla crawling or swimming
 * poses: setupAnim does not call the normal humanoid animation at all, it places every part itself.
 *
 * Where each part goes, for walking and for every pose (sit, loaf, stretch, groom, sleep), is in
 * {@link CatPoses}; {@link CatPoseBlender} fades from one pose to the next. This class only copies the
 * numbers onto the model parts.
 *
 * Model space: 1 unit = 1/16 block, +y is down, the front of the model is -z, the ground is y = 24.
 * Everything is centered on the player's position (the whole model is drawn at CatBody.SCALE).
 */
public class CatPlayerModel extends PlayerModel<AbstractClientPlayer> {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);

    private static final CatPoseBlender BLENDER = new CatPoseBlender();

    private final float[] target = new float[CatPoses.SIZE];
    private final float[] placed = new float[CatPoses.SIZE];

    public CatPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    /** Forgets the fades of all players, e.g. when the resources reload. */
    public static void clearBlends() {
        BLENDER.clear();
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        NekoPose pose = NekoStates.get(player).pose();
        boolean airborne = !player.onGround() && !player.isInWater();
        // In deep water (not wading with the feet on the bottom) the cat paddles like a dog.
        boolean swimming = player.isSwimming() || (player.isInWater() && !player.onGround());
        CatPoses.compute(pose, target, ageInTicks, headPitch * DEG_TO_RAD, netHeadYaw * DEG_TO_RAD,
                new CatPoses.Motion(limbSwing, limbSwingAmount, airborne, this.attackTime,
                        LeapActions.current(player), player.isCrouching(), player.getY() > player.yo,
                        swimming, LeapActions.age(player, ageInTicks)));
        CatPoseBlender.blend(BLENDER.trackFor(player), pose, target, ageInTicks, placed);

        place(this.head, CatPoses.HEAD);
        place(this.body, CatPoses.BODY);
        place(this.rightArm, CatPoses.FRONT_RIGHT);
        place(this.leftArm, CatPoses.FRONT_LEFT);
        place(this.rightLeg, CatPoses.HIND_RIGHT);
        place(this.leftLeg, CatPoses.HIND_LEFT);

        // The skin's second layer follows the part under it.
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }

    private void place(ModelPart part, int index) {
        int o = index * CatPoses.FIELDS;
        part.setPos(placed[o], placed[o + 1], placed[o + 2]);
        part.xRot = placed[o + 3];
        part.yRot = placed[o + 4];
        part.zRot = placed[o + 5];
        part.xScale = placed[o + 6];
        part.yScale = placed[o + 7];
        part.zScale = placed[o + 8];
    }
}
