package io.github.napuruhusu.nekomimi.state;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The mode and emotion of every player, kept by player UUID.
 *
 * The server holds the real state. Every client keeps a copy of the players it can see, filled in
 * by the sync packet. A single-player game has both, so they are kept apart.
 */
public final class NekoStates {
    private static final Map<UUID, NekoState> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, NekoState> CLIENT = new ConcurrentHashMap<>();

    private NekoStates() {
    }

    private static Map<UUID, NekoState> map(boolean clientSide) {
        return clientSide ? CLIENT : SERVER;
    }

    public static NekoState get(Player player) {
        return map(player.level().isClientSide).getOrDefault(player.getUUID(), NekoState.DEFAULT);
    }

    public static NekoState get(boolean clientSide, UUID id) {
        return map(clientSide).getOrDefault(id, NekoState.DEFAULT);
    }

    public static void set(boolean clientSide, UUID id, NekoState state) {
        if (state.equals(NekoState.DEFAULT)) {
            map(clientSide).remove(id);
        } else {
            map(clientSide).put(id, state);
        }
    }

    public static void remove(boolean clientSide, UUID id) {
        map(clientSide).remove(id);
    }

    public static void clear(boolean clientSide) {
        map(clientSide).clear();
    }

    /** True if this player is drawn as the cat body right now (cat mode, and in a pose that allows it). */
    public static boolean usesCatBody(Player player) {
        return get(player).cat() && CatBody.isCatPose(player.getPose()) && !player.isPassenger();
    }
}
