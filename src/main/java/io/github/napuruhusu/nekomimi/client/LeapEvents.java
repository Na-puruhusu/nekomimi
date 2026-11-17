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
 * The cat's long jump (cat mode only): tap the crouch key while sprinting and the cat leaps far
 * forward. No jump key is needed.
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
    private static final double LONG_SPEED = 0.85D;
    private static final double LONG_LIFT = 0.50D;

    private static int ticksSinceSprint = 100;
    private static boolean wasSneaking;

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
            ticksSinceSprint = 100;
            return;
        }

        ticksSinceSprint = player.isSprinting() ? 0 : Math.min(ticksSinceSprint + 1, 100);
        boolean sneaking = player.input.shiftKeyDown;
        boolean pressed = (sneaking && !wasSneaking) || tapped;
        wasSneaking = sneaking;

        if (pressed && minecraft.screen == null && ticksSinceSprint <= SPRINT_TO_CROUCH_TICKS
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
