package io.github.napuruhusu.nekomimi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.napuruhusu.nekomimi.state.CatBody;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;

/**
 * Draws a player in cat mode: the player's own skin on the {@link CatPlayerModel}, smaller than normal.
 * Armor, the item in hand and a head item are drawn by the usual layers, which follow the posed parts.
 * It replaces the normal player renderer only while the player is a cat, see ClientEvents.
 */
public class CatPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, CatPlayerModel> {
    /** Same size scale as a normal player (the vanilla renderer uses it too), times the cat scale. */
    private static final float MODEL_SCALE = 0.9375F * CatBody.SCALE;

    public CatPlayerRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context,
                new CatPlayerModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim),
                0.5F * CatBody.SCALE);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<AbstractClientPlayer>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<AbstractClientPlayer>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    public void render(AbstractClientPlayer player, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        setModelProperties(player);
        super.render(player, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        return player.getSkinTextureLocation();
    }

    @Override
    protected void scale(AbstractClientPlayer player, PoseStack poseStack, float partialTick) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    /** Which parts of the skin are shown, as chosen in the skin settings. Same rules as the vanilla player renderer. */
    private void setModelProperties(AbstractClientPlayer player) {
        CatPlayerModel model = this.getModel();
        if (player.isSpectator()) {
            model.setAllVisible(false);
            model.head.visible = true;
            model.hat.visible = true;
        } else {
            model.setAllVisible(true);
            model.hat.visible = player.isModelPartShown(PlayerModelPart.HAT);
            model.jacket.visible = player.isModelPartShown(PlayerModelPart.JACKET);
            model.leftPants.visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
            model.rightPants.visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
            model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            model.crouching = player.isCrouching();
        }
    }
}
