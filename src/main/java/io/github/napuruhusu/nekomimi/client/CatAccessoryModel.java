package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/** Humanoid-shaped model that only draws the cat ears (head) or the cat tail (body). */
public class CatAccessoryModel extends HumanoidModel<LivingEntity> {
    /** The tail segments from the root to the tip, or null for the ears. */
    private final ModelPart[] tail;

    public CatAccessoryModel(ModelPart root, boolean withTail) {
        super(root);
        if (withTail) {
            tail = new ModelPart[TailSimulation.SEGMENTS];
            ModelPart part = root.getChild("body");
            for (int i = 0; i < tail.length; i++) {
                part = part.getChild(NekoModels.tailPartName(i));
                tail[i] = part;
            }
        } else {
            tail = null;
        }
    }

    /** Bends the tail into the pose the simulation has computed. */
    public void applyTail(TailSimulation simulation) {
        if (tail == null) {
            return;
        }
        for (int i = 0; i < tail.length; i++) {
            tail[i].xRot = simulation.getPitch(i);
            tail[i].zRot = simulation.getSide(i);
        }
    }
}
