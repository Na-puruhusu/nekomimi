package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The two cat renderers, one for each arm width of the skin. Rebuilt whenever the models are reloaded. */
public final class CatPlayerRenderers {
    private static CatPlayerRenderer defaultRenderer;
    private static CatPlayerRenderer slimRenderer;

    private CatPlayerRenderers() {
    }

    static void init(EntityRendererProvider.Context context) {
        defaultRenderer = new CatPlayerRenderer(context, false);
        slimRenderer = new CatPlayerRenderer(context, true);
    }

    /** The renderer for this player's skin, or null before the renderers have been built. */
    static CatPlayerRenderer get(AbstractClientPlayer player) {
        return "slim".equals(player.getModelName()) ? slimRenderer : defaultRenderer;
    }
}
