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
 * Texture sheet is 64x32.
 *
 * UV layout:
 *   ear base      texOffs(0, 0)   3x2x2
 *   ear tip       texOffs(10, 0)  1x1x2
 *   tail fur      texOffs(0, 8)   2x2x2   (main color)
 *   tail stripe   texOffs(0, 12)  2x2x2   (same as fur unless the color has stripes)
 *   tail tip      texOffs(8, 8)   2x2x2
 *
 * The tail is TailSimulation.SEGMENTS boxes, all direct children of the body. They are not nested:
 * CatAccessoryModel works out the position and rotation of every segment from the joint angles
 * each frame. That way a segment can be made wider (puffed fur) without the width stacking up
 * along the chain, which it would if every segment were a child of the one before.
 */
public final class NekoModels {
    public static final ModelLayerLocation EARS_LAYER =
            new ModelLayerLocation(new ResourceLocation(Nekomimi.MODID, "nekomimi"), "main");
    public static final ModelLayerLocation TAIL_LAYER =
            new ModelLayerLocation(new ResourceLocation(Nekomimi.MODID, "neko_tail"), "main");

    private static final int[] FUR = {0, 8};
    private static final int[] STRIPE = {0, 12};
    private static final int[] TIP = {8, 8};
    /** Texture offset of each tail segment, from the root to the tip. */
    private static final int[][] TAIL_UV = {FUR, STRIPE, FUR, STRIPE, TIP, TIP};

    private NekoModels() {
    }

    /** Name of the part for tail segment i (0 = root). All of them are children of the body part. */
    public static String tailPartName(int segment) {
        return "tail_" + segment;
    }

    public static LayerDefinition createEarsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        // The model's +x side is the wearer's left.
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, -10.0F, -3.0F, 3.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("left_ear_tip",
                CubeListBuilder.create().texOffs(10, 0).addBox(2.0F, -11.0F, -3.0F, 1.0F, 1.0F, 2.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -3.0F, 3.0F, 2.0F, 2.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("right_ear_tip",
                CubeListBuilder.create().texOffs(10, 0).addBox(-3.0F, -11.0F, -3.0F, 1.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        addEmpty(root, "hat", PartPose.ZERO);
        addEmpty(root, "body", PartPose.ZERO);
        addEmptyLimbs(root);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createTailLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        addEmpty(root, "head", PartPose.ZERO);
        addEmpty(root, "hat", PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        for (int i = 0; i < TailSimulation.SEGMENTS; i++) {
            // Thinner toward the tip. The extra length overlaps neighbouring segments so no gap
            // shows on the outside of a bend.
            float thickness = 0.1F - 0.04F * i;
            int[] uv = TAIL_UV[i];
            CubeListBuilder cube = CubeListBuilder.create().texOffs(uv[0], uv[1])
                    .addBox(-1.0F, 0.0F, -1.0F, 2.0F, TailSimulation.SEGMENT_LENGTH, 2.0F,
                            new CubeDeformation(thickness, 0.2F, thickness));
            // The pose is set every frame by CatAccessoryModel.applyTail.
            body.addOrReplaceChild(tailPartName(i), cube, PartPose.ZERO);
        }

        addEmptyLimbs(root);
        return LayerDefinition.create(mesh, 64, 32);
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
