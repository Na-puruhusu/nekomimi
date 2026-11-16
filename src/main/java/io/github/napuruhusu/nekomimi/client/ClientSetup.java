package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.Nekomimi;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Nekomimi.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NekoModels.EARS_LAYER, NekoModels::createEarsLayer);
        event.registerLayerDefinition(NekoModels.TAIL_LAYER, NekoModels::createTailLayer);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.EMOTION_MENU);
        event.register(ModKeys.POSE_MENU);
        event.register(ModKeys.TOGGLE_MODE);
    }

    /** Builds the cat renderers. Runs again whenever the models are reloaded. */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        CatPlayerRenderers.init(event.getContext());
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> NekoClientExtensions.clearCache());
    }
}
