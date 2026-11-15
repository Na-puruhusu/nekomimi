package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoState;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Applies a state that arrived from the server. Client only. */
public final class ClientSync {
    private ClientSync() {
    }

    public static void apply(UUID playerId, NekoState state) {
        NekoStates.set(true, playerId, state);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Player player = minecraft.level.getPlayerByUUID(playerId);
            if (player != null) {
                // Recompute the hitbox now that the mode changed.
                player.refreshDimensions();
            }
        }
    }
}
