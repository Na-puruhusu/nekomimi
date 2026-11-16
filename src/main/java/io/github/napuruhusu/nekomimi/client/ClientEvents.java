package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.network.SetStatePacket;
import io.github.napuruhusu.nekomimi.state.NekoEmotion;
import io.github.napuruhusu.nekomimi.state.NekoPose;
import io.github.napuruhusu.nekomimi.state.NekoState;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Client side game events: switching the mode, opening the menu, and drawing the cat. */
@Mod.EventBusSubscriber(modid = Nekomimi.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final List<NekoEmotion> EMOTIONS = List.of(
            NekoEmotion.HAPPY, NekoEmotion.RELAXED, NekoEmotion.ANNOYED, NekoEmotion.SCARED,
            NekoEmotion.SURPRISED, NekoEmotion.INTERESTED, NekoEmotion.AFFECTIONATE);
    private static final List<NekoPose> POSES = List.of(
            NekoPose.SIT, NekoPose.LOAF, NekoPose.STRETCH, NekoPose.GROOM, NekoPose.SLEEP);

    private ClientEvents() {
    }

    /** Alt + right click switches the mode instead of using the item or block. */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && Screen.hasAltDown()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            toggleMode();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (ModKeys.TOGGLE_MODE.consumeClick()) {
            toggleMode();
        }
        while (ModKeys.EMOTION_MENU.consumeClick()) {
            LocalPlayer player = minecraft.player;
            if (player == null || minecraft.screen != null) {
                continue;
            }
            NekoState state = NekoStates.get(player);
            if (state.cat()) {
                minecraft.setScreen(new WheelScreen(Component.translatable("nekomimi.emotion.title"),
                        EMOTIONS.stream().map(e -> (Component) Component.translatable(e.translationKey())).toList(),
                        Component.translatable(NekoEmotion.NONE.translationKey()),
                        EMOTIONS.indexOf(state.emotion()), ModKeys.EMOTION_MENU,
                        index -> sendState(true, index < 0 ? NekoEmotion.NONE : EMOTIONS.get(index), null)));
            } else {
                player.displayClientMessage(Component.translatable("message.nekomimi.need_cat_mode"), true);
            }
        }
        while (ModKeys.POSE_MENU.consumeClick()) {
            LocalPlayer player = minecraft.player;
            if (player == null || minecraft.screen != null) {
                continue;
            }
            NekoState state = NekoStates.get(player);
            if (state.cat()) {
                minecraft.setScreen(new WheelScreen(Component.translatable("nekomimi.pose.title"),
                        POSES.stream().map(p -> (Component) Component.translatable(p.translationKey())).toList(),
                        Component.translatable(NekoPose.NONE.translationKey()),
                        POSES.indexOf(state.pose()), ModKeys.POSE_MENU,
                        index -> sendState(true, null, index < 0 ? NekoPose.NONE : POSES.get(index))));
            } else {
                player.displayClientMessage(Component.translatable("message.nekomimi.need_cat_mode"), true);
            }
        }

        // Moving or jumping ends a pose at once; the server also ends it when the cat has moved.
        LocalPlayer self = minecraft.player;
        if (self != null && minecraft.screen == null) {
            NekoState state = NekoStates.get(self);
            if (state.cat() && state.pose() != NekoPose.NONE
                    && (self.input.forwardImpulse != 0.0F || self.input.leftImpulse != 0.0F || self.input.jumping)) {
                sendState(true, null, NekoPose.NONE);
            }
        }
    }

    /**
     * Asks the server for a new emotion and/or pose. The part left null stays as it is now.
     * The client's numbers are only a request; the server decides.
     */
    private static void sendState(boolean cat, NekoEmotion emotion, NekoPose pose) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        NekoState state = NekoStates.get(player);
        NekoEmotion e = emotion != null ? emotion : state.emotion();
        NekoPose p = pose != null ? pose : state.pose();
        NekoNetwork.CHANNEL.sendToServer(new SetStatePacket(cat, e.ordinal(), p.ordinal()));
    }

    /** Draws a player in cat mode with the cat renderer instead of the normal one. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // The cat renderer is not a PlayerRenderer, so drawing with it does not raise this event again.
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !NekoStates.usesCatBody(player)) {
            return;
        }
        CatPlayerRenderer renderer = CatPlayerRenderers.get(player);
        if (renderer == null) {
            return;
        }
        event.setCanceled(true);
        renderer.render(player, player.getYRot(), event.getPartialTick(),
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        NekoStates.clear(true);
    }

    private static void toggleMode() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }
        boolean cat = NekoStates.get(player).cat();
        NekoNetwork.CHANNEL.sendToServer(new SetStatePacket(!cat, 0, 0));
    }
}
