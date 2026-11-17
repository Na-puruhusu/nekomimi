package io.github.napuruhusu.nekomimi.server;

import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.network.SyncStatePacket;
import io.github.napuruhusu.nekomimi.network.SyncLeapPacket;
import io.github.napuruhusu.nekomimi.state.NekoEmotion;
import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoPose;
import io.github.napuruhusu.nekomimi.state.NekoState;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/** Server side rules for changing the mode and emotion. Never trusts the client's numbers. */
public final class NekoServer {
    /** A stretch lasts this many ticks. */
    private static final int STRETCH_TICKS = 50;
    /** A pose ends when the player has moved farther than this from where it began (blocks, squared). */
    private static final double MOVE_LIMIT_SQR = 0.15D * 0.15D;

    /** Where and when each posing player began their pose. Weak keys let a player who left be forgotten. */
    private static final Map<ServerPlayer, PoseAnchor> ANCHORS = new WeakHashMap<>();

    /** Fewest ticks between two reported leaps of one player (a leap lasts longer than this). */
    private static final int LEAP_COOLDOWN_TICKS = 8;
    private static final Map<ServerPlayer, Integer> LAST_LEAP = new WeakHashMap<>();

    private record PoseAnchor(double x, double y, double z, int tick) {
    }

    private NekoServer() {
    }

    public static void requestState(ServerPlayer player, boolean wantCat, int emotionIndex, int poseIndex) {
        NekoState old = NekoStates.get(false, player.getUUID());
        NekoEmotion emotion = wantCat ? NekoEmotion.byIndex(emotionIndex) : NekoEmotion.NONE;
        NekoPose pose = wantCat ? NekoPose.byIndex(poseIndex) : NekoPose.NONE;
        // Emotions and poses only exist in cat mode: a switch into cat mode starts plain.
        if (wantCat && !old.cat()) {
            emotion = NekoEmotion.NONE;
            pose = NekoPose.NONE;
        }
        // A pose needs solid ground under the cat's feet and no vehicle or water.
        if (pose != NekoPose.NONE && pose != old.pose()
                && (!player.onGround() || player.isInWater() || player.isPassenger() || player.isSleeping())) {
            pose = NekoPose.NONE;
        }
        NekoState next = new NekoState(wantCat, emotion, pose);
        if (next.equals(old)) {
            return;
        }

        // Going back to the normal body needs room to stand up.
        if (old.cat() && !wantCat && !canStandUp(player)) {
            player.displayClientMessage(Component.translatable("message.nekomimi.no_space"), true);
            return;
        }

        applyState(player, next);

        if (old.cat() != next.cat()) {
            player.displayClientMessage(Component.translatable(
                    next.cat() ? "message.nekomimi.mode.cat" : "message.nekomimi.mode.normal"), true);
        }
    }

    /**
     * A player reports a boosted jump. The jump itself is the player's own movement; the server only
     * checks that this is a cat and tells the others, so they can show the motion.
     */
    public static void requestLeap(ServerPlayer player, NekoLeap leap) {
        if (leap == NekoLeap.NONE || !NekoStates.usesCatBody(player)) {
            return;
        }
        Integer last = LAST_LEAP.get(player);
        if (last != null && player.tickCount - last < LEAP_COOLDOWN_TICKS) {
            return;
        }
        LAST_LEAP.put(player, player.tickCount);
        NekoNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncLeapPacket(player.getUUID(), leap.ordinal()));
    }

    private static void applyState(ServerPlayer player, NekoState next) {
        NekoState old = NekoStates.get(false, player.getUUID());
        NekoStates.set(false, player.getUUID(), next);
        if (next.pose() != NekoPose.NONE && next.pose() != old.pose()) {
            ANCHORS.put(player, new PoseAnchor(player.getX(), player.getY(), player.getZ(), player.tickCount));
        } else if (next.pose() == NekoPose.NONE) {
            ANCHORS.remove(player);
        }
        player.refreshDimensions();
        broadcast(player, next);
    }

    /**
     * Ends a pose when the cat moves, jumps, falls or is hurt, and ends a stretch after a moment.
     * Called every tick for every player.
     */
    public static void tickPose(ServerPlayer player) {
        NekoState state = NekoStates.get(false, player.getUUID());
        if (state.pose() == NekoPose.NONE) {
            return;
        }
        PoseAnchor anchor = ANCHORS.get(player);
        if (anchor == null) {
            // Started before the anchor existed (should not happen): anchor here.
            ANCHORS.put(player, new PoseAnchor(player.getX(), player.getY(), player.getZ(), player.tickCount));
            return;
        }
        double dx = player.getX() - anchor.x();
        double dy = player.getY() - anchor.y();
        double dz = player.getZ() - anchor.z();
        boolean moved = dx * dx + dz * dz > MOVE_LIMIT_SQR || Math.abs(dy) > 0.2D;
        boolean hurt = player.hurtTime > 0;
        boolean over = state.pose() == NekoPose.STRETCH && player.tickCount - anchor.tick() > STRETCH_TICKS;
        if (moved || hurt || over || !player.onGround() || player.isInWater() || player.isPassenger()) {
            applyState(player, new NekoState(state.cat(), state.emotion(), NekoPose.NONE));
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
                new SyncStatePacket(player.getUUID(), state.cat(), state.emotion().ordinal(), state.pose().ordinal()));
    }

    /** Tells one viewer about a player who just came into their view. */
    public static void sendTo(ServerPlayer viewer, ServerPlayer subject) {
        NekoState state = NekoStates.get(false, subject.getUUID());
        if (state.equals(NekoState.DEFAULT)) {
            return;
        }
        NekoNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new SyncStatePacket(subject.getUUID(), state.cat(), state.emotion().ordinal(), state.pose().ordinal()));
    }
}
