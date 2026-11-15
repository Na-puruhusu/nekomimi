package io.github.napuruhusu.nekomimi.server;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.state.CatBody;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Nekomimi.MODID)
public final class CommonEvents {
    // Player objects of the server and of the client in a single-player game share an entity id,
    // so they are kept in separate maps. Weak keys let a player who left be forgotten.
    private static final Map<Player, Boolean> APPLIED_SERVER = new WeakHashMap<>();
    private static final Map<Player, Boolean> APPLIED_CLIENT = new WeakHashMap<>();

    private CommonEvents() {
    }

    /** Hitbox and eye height of the cat. */
    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)
                || !CatBody.isCatPose(event.getPose())
                || !NekoStates.get(player).cat()) {
            return;
        }
        boolean crouching = event.getPose() == Pose.CROUCHING;
        event.setNewSize(EntityDimensions.scalable(CatBody.WIDTH,
                crouching ? CatBody.CROUCH_HEIGHT : CatBody.HEIGHT));
        event.setNewEyeHeight(crouching ? CatBody.CROUCH_EYE_HEIGHT : CatBody.EYE_HEIGHT);
    }

    /**
     * Makes the game recompute the hitbox whenever the mode of a player changes, also for a player
     * who was created after the state arrived (respawn, other dimension, coming into view).
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        Map<Player, Boolean> applied = player.level().isClientSide ? APPLIED_CLIENT : APPLIED_SERVER;
        boolean cat = NekoStates.get(player).cat();
        Boolean before = applied.put(player, cat);
        if (before == null ? cat : before != cat) {
            player.refreshDimensions();
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject) {
            NekoServer.sendTo(viewer, subject);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NekoStates.remove(false, event.getEntity().getUUID());
    }
}
