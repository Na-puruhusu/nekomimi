package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.network.LeapPacket;
import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The boosted jumps of the cat (only in cat mode). Crouch, let go quickly, then jump:
 * <ul>
 *   <li>long jump: if the crouch began right after sprinting (the sprint stops when you crouch)</li>
 *   <li>high jump: if it began otherwise, standing or walking</li>
 * </ul>
 * The jump is the player's own movement, so it is decided and applied here on the client. The server is
 * only told, so that other players can see the motion (see LeapPacket).
 */
@Mod.EventBusSubscriber(modid = Nekomimi.MODID, value = Dist.CLIENT)
public final class LeapEvents {
    /** A crouch that begins this many ticks after the sprint ended still counts as "from the sprint". */
    private static final int SPRINT_TO_CROUCH_TICKS = 4;
    /** The crouch must be held at least this long (not just a tap of the key) and at most this long. */
    private static final int MIN_CROUCH_TICKS = 2;
    private static final int MAX_CROUCH_TICKS = 60;
    /** After letting go of crouch, the jump must come within this many ticks. */
    private static final int JUMP_WINDOW_TICKS = 7;

    /** Long jump: speed forward and the upward speed (an ordinary jump is 0.42; the air slows it down by a tenth each tick). */
    private static final double LONG_SPEED = 0.85D;
    private static final double LONG_LIFT = 0.50D;
    /** High jump: upward speed, about 2.8 blocks high (an ordinary jump reaches 1.25). Low enough to land without fall damage. */
    private static final double HIGH_LIFT = 0.66D;
    /** The high jump keeps this much of the horizontal speed the player had. */
    private static final double HIGH_KEEP = 0.7D;

    private static int ticksSinceSprint = 100;
    private static boolean crouchHeld;
    private static int crouchTicks;
    /** What the running crouch is turning into: decided when the crouch begins. */
    private static NekoLeap charging = NekoLeap.NONE;
    /** A finished crouch that is waiting for the jump. */
    private static NekoLeap armed = NekoLeap.NONE;
    private static int sinceRelease = 100;

    private LeapEvents() {
    }

    private static void reset() {
        crouchHeld = false;
        crouchTicks = 0;
        charging = NekoLeap.NONE;
        armed = NekoLeap.NONE;
        sinceRelease = 100;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !NekoStates.get(player).cat()) {
            reset();
            return;
        }

        ticksSinceSprint = player.isSprinting() ? 0 : Math.min(ticksSinceSprint + 1, 100);
        boolean sneaking = player.input.shiftKeyDown;

        if (sneaking) {
            if (!crouchHeld) {
                crouchHeld = true;
                crouchTicks = 0;
                armed = NekoLeap.NONE;
                if (!player.onGround() || LeapActions.current(player) != NekoLeap.NONE) {
                    charging = NekoLeap.NONE;
                } else {
                    charging = ticksSinceSprint <= SPRINT_TO_CROUCH_TICKS ? NekoLeap.LONG : NekoLeap.HIGH;
                }
            } else {
                crouchTicks++;
            }
        } else if (crouchHeld) {
            crouchHeld = false;
            sinceRelease = 0;
            armed = crouchTicks >= MIN_CROUCH_TICKS && crouchTicks <= MAX_CROUCH_TICKS ? charging : NekoLeap.NONE;
            charging = NekoLeap.NONE;
        } else {
            sinceRelease = Math.min(sinceRelease + 1, 100);
            if (sinceRelease > JUMP_WINDOW_TICKS) {
                armed = NekoLeap.NONE;
            }
        }
    }

    /** Called by the game right after a jump has given the player its jumping speed. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || player != Minecraft.getInstance().player
                || armed == NekoLeap.NONE || !NekoStates.get(player).cat()) {
            return;
        }
        NekoLeap leap = armed;
        armed = NekoLeap.NONE;

        Vec3 v = player.getDeltaMovement();
        if (leap == NekoLeap.LONG) {
            double yaw = Math.toRadians(player.getYRot());
            player.setDeltaMovement(-Math.sin(yaw) * LONG_SPEED, LONG_LIFT, Math.cos(yaw) * LONG_SPEED);
        } else {
            player.setDeltaMovement(v.x * HIGH_KEEP, HIGH_LIFT, v.z * HIGH_KEEP);
        }
        LeapActions.start(player, leap);
        NekoNetwork.CHANNEL.sendToServer(new LeapPacket(leap.ordinal()));
    }
}
