package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoState;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Applies a state that arrived from the server. Client only. */
public final class ClientSync {
    private ClientSync() {
    }

    /** Another player made a boosted jump. (The player's own jump was started on their own client already.) */
    public static void applyLeap(UUID playerId, NekoLeap leap, int extra) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.getUUID().equals(playerId)) {
            return;
        }
        Player player = minecraft.level.getPlayerByUUID(playerId);
        if (player != null) {
            LeapActions.start(player, leap, extra);
        }
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
