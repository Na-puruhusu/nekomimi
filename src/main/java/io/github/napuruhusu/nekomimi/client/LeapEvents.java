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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The cat's long jump (cat mode only): press the crouch key while sprinting and let go again quickly,
 * and the cat leaps far forward when the key comes up. No jump key is needed. Holding the key for
 * longer is just a normal crouch.
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

    /** The key must be let go within this many ticks of being pressed (5 ticks = 0.25 seconds). */
    private static final int MAX_HOLD_TICKS = 5;

    /** Speed forward and upward speed of the leap (an ordinary jump is 0.42 up; the air slows the forward speed by a tenth each tick). */
    private static final double LONG_SPEED = 0.85D;
    private static final double LONG_LIFT = 0.50D;

    private static int ticksSinceSprint = 100;
    private static boolean wasSneaking;
    /** Whether the running crouch began right after a sprint, and for how many ticks it has been held. */
    private static boolean fromSprint;
    private static int heldTicks;

    private LeapEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // The game only looks at whether the crouch key is down at the moment of a tick, so a very quick tap
        // can pass between two ticks. The key also counts its presses; they are read here (and used up).
        boolean tapped = false;
        while (minecraft.options.keyShift.consumeClick()) {
            tapped = true;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || !NekoStates.get(player).cat()) {
            wasSneaking = false;
            fromSprint = false;
            ticksSinceSprint = 100;
            return;
        }

        ticksSinceSprint = player.isSprinting() ? 0 : Math.min(ticksSinceSprint + 1, 100);
        boolean sneaking = player.input.shiftKeyDown;
        boolean released = false;

        if (sneaking) {
            if (!wasSneaking) {
                // The crouch begins: remember whether it follows a sprint.
                fromSprint = ticksSinceSprint <= SPRINT_TO_CROUCH_TICKS;
                heldTicks = 0;
            } else {
                heldTicks++;
            }
        } else if (wasSneaking) {
            released = fromSprint && heldTicks <= MAX_HOLD_TICKS;
            fromSprint = false;
        } else if (tapped) {
            // A tap that began and ended between two ticks: as short as a press can be.
            released = ticksSinceSprint <= SPRINT_TO_CROUCH_TICKS;
        }
        wasSneaking = sneaking;

        if (released && minecraft.screen == null
                && player.onGround() && !player.isInWater() && !player.isPassenger()
                && !player.isFallFlying() && !player.isSleeping()
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
