package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.Nekomimi;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Model layer definitions (geometry) for the cat ears and the cat tail.
 *
 * Both are HumanoidModel-shaped meshes whose unused parts are empty, so that the
 * pose of the wearer can be copied onto them like a normal armor model.
 *
 * The fur is made of shells, see {@link FurLayout}: every ear piece and every tail segment is
 * FurLayout.LAYERS boxes of growing size. The boxes are TEXELS times larger than they are shown
 * and the model code scales them down by PART_SCALE (CatAccessoryModel), so all sizes below are in texels.
 * Where each box finds its texture is in FurLayout.
 *
 * The tail is TailSimulation.SEGMENTS segments, all direct children of the body. They are not nested:
 * CatAccessoryModel works out the position and rotation of every segment from the joint angles
 * each frame. That way a segment can be made wider (puffed fur) without the width stacking up
 * along the chain, which it would if every segment were a child of the one before.
 */
public final class NekoModels {
    public static final ModelLayerLocation EARS_LAYER =
            new ModelLayerLocation(new ResourceLocation(Nekomimi.MODID, "nekomimi"), "main");
    public static final ModelLayerLocation TAIL_LAYER =
            new ModelLayerLocation(new ResourceLocation(Nekomimi.MODID, "neko_tail"), "main");

    private static final int FUR = FurLayout.TAIL_VARIANT_FUR;
    private static final int STRIPE = FurLayout.TAIL_VARIANT_STRIPE;
    private static final int TIP = FurLayout.TAIL_VARIANT_TIP;
    /** Texture variant of each tail segment, from the root to the tip. */
    private static final int[] TAIL_VARIANT = {FUR, STRIPE, FUR, STRIPE, TIP, TIP};

    /**
     * Centers of the ear pieces in the head frame, in model units, [piece][side]. Each piece is a box
     * around its own center so that it can be scaled about it. The model's +x side is the wearer's left.
     * The head top is at y = -8: the pieces stack upward from there and narrow to a point.
     */
    static final float[][][] EAR_CENTER = {
            {{2.5F, -8.75F, -2.0F}, {-2.5F, -8.75F, -2.0F}},
            {{2.5F, -10.25F, -2.0F}, {-2.5F, -10.25F, -2.0F}},
            {{2.5F, -11.5F, -2.0F}, {-2.5F, -11.5F, -2.0F}},
    };
    static final String[] EAR_SIDE = {"left", "right"};

    private NekoModels() {
    }

    /** Name of the part for tail segment i (0 = root), layer k (0 = base). All of them are children of the body part. */
    public static String tailPartName(int segment, int layer) {
        return "tail_" + segment + "_" + layer;
    }

    /** Name of an ear piece (0 = bottom). side 0 = left, 1 = right. A child of the head part. */
    public static String earPartName(int side, int piece, int layer) {
        return EAR_SIDE[side] + "_ear_" + piece + "_" + layer;
    }

    /** Extra half-width of a tail segment's base box, in texels (thinner toward the tip). */
    static float tailThickness(int segment) {
        return (0.1F - 0.04F * segment) * FurLayout.TEXELS;
    }

    public static LayerDefinition createEarsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        for (int side = 0; side < 2; side++) {
            for (int piece = 0; piece < FurLayout.EAR_PIECES; piece++) {
                int[] box = FurLayout.EAR_BOX[piece];
                float[] c = EAR_CENTER[piece][side];
                for (int layer = 0; layer < FurLayout.LAYERS; layer++) {
                    CubeDeformation deformation = new CubeDeformation(FurLayout.grow(layer, FurLayout.EAR_FUR_LENGTH));
                    head.addOrReplaceChild(earPartName(side, piece, layer),
                            CubeListBuilder.create().texOffs(FurLayout.earU(piece, layer), FurLayout.earV(piece, layer))
                                    .addBox(-box[0] / 2.0F, -box[1] / 2.0F, -box[2] / 2.0F,
                                            box[0], box[1], box[2], deformation),
                            PartPose.offset(c[0], c[1], c[2]));
                }
            }
        }

        addEmpty(root, "hat", PartPose.ZERO);
        addEmpty(root, "body", PartPose.ZERO);
        addEmptyLimbs(root);
        return LayerDefinition.create(mesh, FurLayout.EAR_TEX_WIDTH, FurLayout.EAR_TEX_HEIGHT);
    }

    public static LayerDefinition createTailLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        addEmpty(root, "head", PartPose.ZERO);
        addEmpty(root, "hat", PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        float half = FurLayout.TAIL_BOX_WIDTH / 2.0F;
        // One segment is SEGMENT_LENGTH units long, which is TAIL_BOX_HEIGHT texels.
        float length = FurLayout.TAIL_BOX_HEIGHT;
        for (int i = 0; i < TailSimulation.SEGMENTS; i++) {
            float thickness = tailThickness(i);
            for (int layer = 0; layer < FurLayout.LAYERS; layer++) {
                float grow = FurLayout.grow(layer, FurLayout.TAIL_FUR_LENGTH);
                // The base box is a little longer than the segment so that no gap shows on the outside of a
                // bend; the fur grows less along the segment than around it.
                CubeDeformation deformation = new CubeDeformation(
                        thickness + grow, 0.2F * FurLayout.TEXELS + 0.5F * grow, thickness + grow);
                int variant = TAIL_VARIANT[i];
                // The pose is set every frame by CatAccessoryModel.applyTail.
                body.addOrReplaceChild(tailPartName(i, layer),
                        CubeListBuilder.create().texOffs(FurLayout.tailU(variant, layer), FurLayout.tailV(variant, layer))
                                .addBox(-half, 0.0F, -half, FurLayout.TAIL_BOX_WIDTH, length,
                                        FurLayout.TAIL_BOX_WIDTH, deformation),
                        PartPose.ZERO);
            }
        }

        addEmptyLimbs(root);
        return LayerDefinition.create(mesh, FurLayout.TAIL_TEX_WIDTH, FurLayout.TAIL_TEX_HEIGHT);
    }

    private static void addEmpty(PartDefinition root, String name, PartPose pose) {
        root.addOrReplaceChild(name, CubeListBuilder.create(), pose);
    }

    /** HumanoidModel requires all of these parts to exist. */
    private static void addEmptyLimbs(PartDefinition root) {
        addEmpty(root, "right_arm", PartPose.offset(-5.0F, 2.0F, 0.0F));
        addEmpty(root, "left_arm", PartPose.offset(5.0F, 2.0F, 0.0F));
        addEmpty(root, "right_leg", PartPose.offset(-1.9F, 12.0F, 0.0F));
        addEmpty(root, "left_leg", PartPose.offset(1.9F, 12.0F, 0.0F));
    }
}
