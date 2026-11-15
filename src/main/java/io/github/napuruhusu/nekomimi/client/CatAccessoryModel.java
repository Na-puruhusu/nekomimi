package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/** Humanoid-shaped model that only draws the cat ears (head) or the cat tail (body). */
public class CatAccessoryModel extends HumanoidModel<LivingEntity> {
    /** Where the tail is attached, in the frame of the body part (the lower back). */
    private static final float ROOT_X = 0.0F;
    private static final float ROOT_Y = 10.0F;
    private static final float ROOT_Z = 2.0F;

    /** The tail segments from the root to the tip, or null for the ears. */
    private final ModelPart[] tail;

    public CatAccessoryModel(ModelPart root, boolean withTail) {
        super(root);
        if (withTail) {
            tail = new ModelPart[TailSimulation.SEGMENTS];
            ModelPart body = root.getChild("body");
            for (int i = 0; i < tail.length; i++) {
                tail[i] = body.getChild(NekoModels.tailPartName(i));
            }
        } else {
            tail = null;
        }
    }

    /**
     * Puts every tail segment where the simulation says it is.
     *
     * Each joint turns the chain by Rz(side) * Rx(pitch), in the frame of the segment before it.
     * The orientation of a segment is the product of all joints up to it, and the next segment
     * starts one segment length along its own y axis. The orientation is then converted to the
     * Euler angles a ModelPart uses (it rotates by Rz * Ry * Rx).
     */
    public void applyTail(TailSimulation simulation) {
        if (tail == null) {
            return;
        }
        // Row-major 3x3 orientation, starting as the identity.
        float[] o = {1, 0, 0, 0, 1, 0, 0, 0, 1};
        float x = ROOT_X;
        float y = ROOT_Y;
        float z = ROOT_Z;
        float width = simulation.getThickness();

        for (int i = 0; i < tail.length; i++) {
            float cs = (float) Math.cos(simulation.getSide(i));
            float ss = (float) Math.sin(simulation.getSide(i));
            float cp = (float) Math.cos(simulation.getPitch(i));
            float sp = (float) Math.sin(simulation.getPitch(i));
            // joint = Rz(side) * Rx(pitch)
            float[] joint = {
                    cs, -ss * cp, ss * sp,
                    ss, cs * cp, -cs * sp,
                    0, sp, cp};
            o = multiply(o, joint);

            ModelPart part = tail[i];
            part.setPos(x, y, z);
            // Rz(g) * Ry(b) * Rx(a): m20 = -sin b, m21 = cos b sin a, m22 = cos b cos a, m10 = sin g cos b, m00 = cos g cos b
            float beta = (float) -Math.asin(Math.max(-1.0F, Math.min(1.0F, o[6])));
            if (Math.abs(o[6]) < 0.9999F) {
                part.xRot = (float) Math.atan2(o[7], o[8]);
                part.zRot = (float) Math.atan2(o[3], o[0]);
            } else {
                // Looking straight along an axis: only the sum of the two angles is defined.
                part.xRot = (float) Math.atan2(-o[5], o[4]);
                part.zRot = 0.0F;
            }
            part.yRot = beta;
            part.xScale = width;
            part.zScale = width;

            // The next segment starts at the far end of this one: orientation * (0, length, 0).
            x += o[1] * TailSimulation.SEGMENT_LENGTH;
            y += o[4] * TailSimulation.SEGMENT_LENGTH;
            z += o[7] * TailSimulation.SEGMENT_LENGTH;
        }
    }

    private static float[] multiply(float[] a, float[] b) {
        float[] r = new float[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                r[row * 3 + col] = a[row * 3] * b[col] + a[row * 3 + 1] * b[3 + col] + a[row * 3 + 2] * b[6 + col];
            }
        }
        return r;
    }
}
