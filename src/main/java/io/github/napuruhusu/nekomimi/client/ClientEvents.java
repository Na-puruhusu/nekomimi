package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.network.SetStatePacket;
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

/** Client side game events: switching the mode, opening the menu, and drawing the cat. */
@Mod.EventBusSubscriber(modid = Nekomimi.MODID, value = Dist.CLIENT)
public final class ClientEvents {
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
                minecraft.setScreen(new EmotionWheelScreen(state.emotion()));
            } else {
                player.displayClientMessage(Component.translatable("message.nekomimi.need_cat_mode"), true);
            }
        }
    }

    /** Draws a player in cat mode with the cat renderer instead of the normal one. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // The cat renderer is itself a player renderer and raises this event too. Let it through.
        if (event.getRenderer() instanceof CatPlayerRenderer
                || !(event.getEntity() instanceof AbstractClientPlayer player)
                || !NekoStates.usesCatBody(player)) {
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
        NekoNetwork.CHANNEL.sendToServer(new SetStatePacket(!cat, 0));
    }
}
