package io.github.napuruhusu.nekomimi.server;

import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.network.SyncStatePacket;
import io.github.napuruhusu.nekomimi.state.NekoEmotion;
import io.github.napuruhusu.nekomimi.state.NekoState;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

/** Server side rules for changing the mode and emotion. Never trusts the client's numbers. */
public final class NekoServer {
    private NekoServer() {
    }

    public static void requestState(ServerPlayer player, boolean wantCat, int emotionIndex) {
        NekoState old = NekoStates.get(false, player.getUUID());
        NekoEmotion emotion = wantCat ? NekoEmotion.byIndex(emotionIndex) : NekoEmotion.NONE;
        // Emotions only exist in cat mode.
        if (wantCat && !old.cat() && emotion != NekoEmotion.NONE) {
            emotion = NekoEmotion.NONE;
        }
        NekoState next = new NekoState(wantCat, emotion);
        if (next.equals(old)) {
            return;
        }

        // Going back to the normal body needs room to stand up.
        if (old.cat() && !wantCat && !canStandUp(player)) {
            player.displayClientMessage(Component.translatable("message.nekomimi.no_space"), true);
            return;
        }

        NekoStates.set(false, player.getUUID(), next);
        player.refreshDimensions();
        broadcast(player, next);

        if (old.cat() != next.cat()) {
            player.displayClientMessage(Component.translatable(
                    next.cat() ? "message.nekomimi.mode.cat" : "message.nekomimi.mode.normal"), true);
        }
    }

    /**
     * Whether the normal standing body would fit where the player is. getDimensions(Pose) is the
     * vanilla size and does not include the smaller cat hitbox, so it works while still in cat mode.
     */
    private static boolean canStandUp(ServerPlayer player) {
        AABB standing = player.getDimensions(Pose.STANDING).makeBoundingBox(player.position());
        return player.level().noCollision(player, standing.deflate(1.0E-7D));
    }

    /** Sends the state of this player to the player and to everyone tracking them. */
    public static void broadcast(ServerPlayer player, NekoState state) {
        NekoNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncStatePacket(player.getUUID(), state.cat(), state.emotion().ordinal()));
    }

    /** Tells one viewer about a player who just came into their view. */
    public static void sendTo(ServerPlayer viewer, ServerPlayer subject) {
        NekoState state = NekoStates.get(false, subject.getUUID());
        if (state.equals(NekoState.DEFAULT)) {
            return;
        }
        NekoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new SyncStatePacket(subject.getUUID(), state.cat(), state.emotion().ordinal()));
    }
}
