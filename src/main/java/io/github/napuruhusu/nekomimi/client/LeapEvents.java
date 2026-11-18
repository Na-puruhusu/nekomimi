package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.network.LeapPacket;
import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The cat's long jump (cat mode only): crouch while sprinting, and jump while still crouching. The
 * jump then goes far forward. A crouch that did not begin right after a sprint gives an ordinary jump.
 *
 * The jump is the player's own movement, so it is decided and applied here on the client. The server is
 * only told, so that other players can see the motion (see LeapPacket).
 */
@Mod.EventBusSubscriber(modid = Nekomimi.MODID, value = Dist.CLIENT)
public final class LeapEvents {
    /**
     * The sprint stops the moment you crouch, so a crouch counts as "while sprinting" if the sprint
     * ended no more than this many ticks ago.
     */
    private static final int SPRINT_TO_CROUCH_TICKS = 4;

    /** Speed forward and upward speed of the leap (an ordinary jump is 0.42 up; the air slows the forward speed by a tenth each tick). */
    static final double LONG_SPEED = 0.85D;
    static final double LONG_LIFT = 0.50D;

    private static int ticksSinceSprint = 100;
    private static boolean wasSneaking;
    /** Whether the running crouch began right after a sprint. */
    private static boolean fromSprint;

    private LeapEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !NekoStates.get(player).cat()) {
            wasSneaking = false;
            fromSprint = false;
            ticksSinceSprint = 100;
            LeapPreview.clear();
            return;
        }

        ticksSinceSprint = player.isSprinting() ? 0 : Math.min(ticksSinceSprint + 1, 100);
        boolean sneaking = player.input.shiftKeyDown;

        if (sneaking && !wasSneaking) {
            // The crouch begins: remember whether it follows a sprint.
            fromSprint = ticksSinceSprint <= SPRINT_TO_CROUCH_TICKS;
        } else if (!sneaking) {
            fromSprint = false;
        }
        wasSneaking = sneaking;

        // While the long jump is ready, show where it would land.
        boolean ready = sneaking && (fromSprint || ticksSinceSprint <= SPRINT_TO_CROUCH_TICKS)
                && player.onGround() && !player.isInWater() && !player.isPassenger()
                && minecraft.screen == null && LeapActions.current(player) == NekoLeap.NONE;
        LeapPreview.update(player, ready);
    }

    /** Called by the game right after a jump has given the player its jumping speed. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player
                || !NekoStates.get(player).cat() || minecraft.screen != null) {
            return;
        }
        // Still crouching, and the crouch began right after a sprint. (A crouch pressed in the very tick of the
        // jump has not been seen by the tick handler yet, so the sprint that just ended counts too.)
        boolean crouching = player.input.shiftKeyDown && (fromSprint || ticksSinceSprint <= SPRINT_TO_CROUCH_TICKS);
        if (crouching && !player.isInWater() && !player.isPassenger()
                && LeapActions.current(player) == NekoLeap.NONE) {
            leap(player);
        }
    }

    private static void leap(LocalPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        player.setDeltaMovement(-Math.sin(yaw) * LONG_SPEED, LONG_LIFT, Math.cos(yaw) * LONG_SPEED);
        player.hasImpulse = true;
        LeapActions.start(player, NekoLeap.LONG);
        NekoNetwork.CHANNEL.sendToServer(new LeapPacket(NekoLeap.LONG.ordinal()));
    }
}
