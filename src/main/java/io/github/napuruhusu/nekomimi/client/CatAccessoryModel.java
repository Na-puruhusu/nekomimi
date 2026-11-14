package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Humanoid-shaped model that only draws the cat ears (head) or the cat tail (body). */
public class CatAccessoryModel extends HumanoidModel<LivingEntity> {
    private final ModelPart tailRoot;

    public CatAccessoryModel(ModelPart root, boolean withTail) {
        super(root);
        this.tailRoot = withTail ? root.getChild("body").getChild("tail_root") : null;
    }

    /** Gentle idle sway of the tail. */
    public void animateTail(LivingEntity entity) {
        if (tailRoot == null) {
            return;
        }
        float time = entity.tickCount + Minecraft.getInstance().getFrameTime();
        tailRoot.yRot = Mth.sin(time * 0.09F) * 0.22F;
        tailRoot.zRot = Mth.sin(time * 0.045F) * 0.06F;
    }
}
