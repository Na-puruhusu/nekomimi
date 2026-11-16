package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * Humanoid-shaped model that only draws the cat ears (head) or the cat tail (body).
 *
 * Both are made of fur shells (see {@link FurLayout}): FurLayout.LAYERS boxes per part, built
 * TEXELS times too large and scaled down here. Each frame the shells of a part get the same pose and
 * are then pushed apart a little by the fur sway of the wearer's motion (outer layers more), and
 * stretched or shrunk for the length of the fur (long when startled, sleek when relaxed).
 */
public class CatAccessoryModel extends HumanoidModel<LivingEntity> {
    /** Where the tail is attached, in the frame of the body part (the lower back). */
    private static final float ROOT_X = 0.0F;
    private static final float ROOT_Y = 10.0F;
    private static final float ROOT_Z = 2.0F;

    /** How much of the tail's fur sway the ears get (their fur is shorter). */
    private static final float EAR_SWAY = 0.6F;

    /** The tail segments from the root to the tip, each with its fur layers, or null for the ears. */
    private final ModelPart[][] tail;
    /** The ear pieces: [side][piece][layer], or null for the tail. */
    private final ModelPart[][][] ears;

    public CatAccessoryModel(ModelPart root, boolean withTail) {
        super(root);
        if (withTail) {
            tail = new ModelPart[TailSimulation.SEGMENTS][FurLayout.LAYERS];
            ModelPart body = root.getChild("body");
            for (int i = 0; i < tail.length; i++) {
                for (int k = 0; k < FurLayout.LAYERS; k++) {
                    tail[i][k] = body.getChild(NekoModels.tailPartName(i, k));
                }
            }
            ears = null;
        } else {
            tail = null;
            ears = new ModelPart[2][FurLayout.EAR_PIECES][FurLayout.LAYERS];
            ModelPart head = root.getChild("head");
            for (int side = 0; side < 2; side++) {
                for (int piece = 0; piece < FurLayout.EAR_PIECES; piece++) {
                    for (int k = 0; k < FurLayout.LAYERS; k++) {
                        ears[side][piece][k] = head.getChild(NekoModels.earPartName(side, piece, k));
                    }
                }
            }
        }
    }

    /**
     * Sets the fur of the ears for this frame: how long it is and how far the wearer's motion has pushed it.
     * The simulation is the wearer's tail simulation, which also tracks the fur.
     */
    public void applyEars(TailSimulation simulation) {
        if (ears == null) {
            return;
        }
        float furLength = simulation.getFurLength();
        for (int side = 0; side < 2; side++) {
            // Each ear turns as one piece about the middle of its base on the head: forward and back
            // (pitch), and tip away from or toward the head (out; the right ear leans the other way).
            float pitch = simulation.getEarPitch(side);
            float roll = simulation.getEarOut(side) * (side == 0 ? 1.0F : -1.0F);
            float cx = (float) Math.cos(pitch);
            float sx = (float) Math.sin(pitch);
            float cz = (float) Math.cos(roll);
            float sz = (float) Math.sin(roll);
            float[] pivot = NekoModels.EAR_PIVOT[side];

            for (int piece = 0; piece < FurLayout.EAR_PIECES; piece++) {
                float[] center = NekoModels.EAR_CENTER[piece][side];
                int[] box = FurLayout.EAR_BOX[piece];
                // Where the piece's center goes: its offset from the pivot, turned by Rx(pitch) and then Rz(roll).
                float rx = center[0] - pivot[0];
                float ry = center[1] - pivot[1];
                float rz = center[2] - pivot[2];
                float y1 = ry * cx - rz * sx;
                float z1 = ry * sx + rz * cx;
                float x2 = rx * cz - y1 * sz;
                float y2 = rx * sz + y1 * cz;
                float px = pivot[0] + x2;
                float py = pivot[1] + y2;
                float pz = pivot[2] + z1;

                for (int k = 0; k < FurLayout.LAYERS; k++) {
                    float grow = FurLayout.grow(k, FurLayout.EAR_FUR_LENGTH);
                    ModelPart part = ears[side][piece][k];
                    float reach = (float) Math.pow(FurLayout.depth(k), 1.5D) * EAR_SWAY;
                    part.setPos(px + simulation.getFurOffset(0) * reach,
                            py + simulation.getFurOffset(1) * reach,
                            pz + simulation.getFurOffset(2) * reach);
                    part.xRot = pitch;
                    part.yRot = 0.0F;
                    part.zRot = roll;
                    part.xScale = FurLayout.PART_SCALE * stretch(box[0] / 2.0F, grow, furLength);
                    part.yScale = FurLayout.PART_SCALE * stretch(box[1] / 2.0F, grow, furLength);
                    part.zScale = FurLayout.PART_SCALE * stretch(box[2] / 2.0F, grow, furLength);
                }
            }
        }
    }

    /** Scale that turns a shell grown by `grow` texels around a box of half-size `half` into one whose fur is `furLength` times as long. */
    private static float stretch(float half, float grow, float furLength) {
        float size = half + grow;
        return size > 0.0F ? (half + grow * furLength) / size : 1.0F;
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
        boolean catFrame = simulation.getStyle().catFrame;

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

            // Rz(g) * Ry(b) * Rx(a): m20 = -sin b, m21 = cos b sin a, m22 = cos b cos a, m10 = sin g cos b, m00 = cos g cos b
            float beta = (float) -Math.asin(Math.max(-1.0F, Math.min(1.0F, o[6])));
            float alpha;
            float gamma;
            if (Math.abs(o[6]) < 0.9999F) {
                alpha = (float) Math.atan2(o[7], o[8]);
                gamma = (float) Math.atan2(o[3], o[0]);
            } else {
                // Looking straight along an axis: only the sum of the two angles is defined.
                alpha = (float) Math.atan2(-o[5], o[4]);
                gamma = 0.0F;
            }

            float half = FurLayout.TAIL_BOX_WIDTH / 2.0F + NekoModels.tailThickness(i);
            for (int k = 0; k < FurLayout.LAYERS; k++) {
                ModelPart part = tail[i][k];
                float reach = (float) Math.pow(FurLayout.depth(k), 1.5D);
                // The fur offset is given for an upright body. The cat body lies flat (turned a quarter
                // turn about x), so there the model's y and z take each other's place.
                float ox = simulation.getFurOffset(0);
                float oy = catFrame ? simulation.getFurOffset(2) : simulation.getFurOffset(1);
                float oz = catFrame ? -simulation.getFurOffset(1) : simulation.getFurOffset(2);
                part.setPos(x + ox * reach, y + oy * reach, z + oz * reach);
                part.xRot = alpha;
                part.yRot = beta;
                part.zRot = gamma;
                float grow = FurLayout.grow(k, FurLayout.TAIL_FUR_LENGTH);
                float around = FurLayout.PART_SCALE * width * stretch(half, grow, simulation.getFurLength());
                part.xScale = around;
                part.yScale = FurLayout.PART_SCALE;
                part.zScale = around;
            }

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
