package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.Nekomimi;
import io.github.napuruhusu.nekomimi.network.LeapPacket;
import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.state.CatBody;
import io.github.napuruhusu.nekomimi.state.NekoLeap;
import io.github.napuruhusu.nekomimi.state.NekoStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * The cat hang (cat mode only). While the cat is in a long jump and the use key (right click) is held, a
 * ledge in front of it, about the height of its paws, is caught: the cat hangs there. Jumping while hanging
 * pulls the cat up onto the ledge; crouching lets it drop.
 *
 * During a long jump, and while hanging or climbing, the use and attack keys do nothing (no placing blocks,
 * no attacking), so that the right click can be used for catching a ledge.
 *
 * Like the jump, this is the player's own movement and is decided on the client; the server is only told
 * (LeapPacket) so that other players see the pose. The hang is not a flying kick for the server because
 * the cat always hangs right against the wall.
 */
@Mod.EventBusSubscriber(modid = Nekomimi.MODID, value = Dist.CLIENT)
public final class HangEvents {
    private static final double HALF_WIDTH = CatBody.WIDTH / 2.0D;
    /** Gap between the cat and the wall (the cat must stay within 1/16 of a block of it). */
    private static final double WALL_GAP = 0.02D;
    /** How far the paws may be from the wall when the ledge is caught, beyond the cat's own size. */
    private static final double REACH = 0.25D;
    /** The cat hangs this far below the top of the ledge (its feet, measured from the ledge top). */
    private static final double HANG_BELOW = 0.6D;
    /** The ledge top may be this far above the cat's feet when it is caught (min, max). */
    private static final double CATCH_LOW = 0.2D;
    private static final double CATCH_HIGH = 1.1D;

    private static final int CLIMB_TICKS = (int) CatPoses.CLIMB_TICKS;
    /** Fraction of the climb that goes straight up; the rest moves forward onto the ledge. */
    private static final double CLIMB_UP_PART = 0.55D;

    private static final class Hang {
        final Direction facing;
        final BlockPos ledge;
        final double top;
        final Vec3 anchor;
        final Vec3 dest;
        int climbTick = -1;
        /** The jump key must be let go once before it can start the climb (it may still be held from the leap). */
        boolean jumpReleased;
        int age;

        Hang(Direction facing, BlockPos ledge, double top, Vec3 anchor, Vec3 dest) {
            this.facing = facing;
            this.ledge = ledge;
            this.top = top;
            this.anchor = anchor;
            this.dest = dest;
        }
    }

    private static Hang hang;

    private HangEvents() {
    }

    private static boolean leaping(LocalPlayer player) {
        return NekoStates.get(player).cat() && LeapActions.current(player) != NekoLeap.NONE;
    }

    // ---- catching a ledge ----

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !NekoStates.get(player).cat()) {
            hang = null;
            return;
        }
        if (leaping(player) && minecraft.gameMode != null && minecraft.options.keyAttack.isDown()) {
            // Breaking a block goes on while the key is held, so it has to be stopped here.
            minecraft.gameMode.stopDestroyBlock();
        }
        if (hang == null && LeapActions.current(player) == NekoLeap.LONG && !player.onGround()
                && !player.isInWater() && minecraft.screen == null && minecraft.options.keyUse.isDown()) {
            tryCatch(player);
        }
    }

    private static void tryCatch(LocalPlayer player) {
        Level level = player.level();
        Direction facing = Direction.fromYRot(player.getYRot());
        double reach = HALF_WIDTH + REACH;
        BlockPos column = BlockPos.containing(player.getX() + facing.getStepX() * reach, player.getY(),
                player.getZ() + facing.getStepZ() * reach);
        double feet = player.getY();

        for (int dy = -1; dy <= 2; dy++) {
            BlockPos ledge = new BlockPos(column.getX(), Mth.floor(feet) + dy, column.getZ());
            BlockState state = level.getBlockState(ledge);
            if (!state.isFaceSturdy(level, ledge, Direction.UP)
                    || !state.isFaceSturdy(level, ledge, facing.getOpposite())) {
                continue;
            }
            BlockPos above = ledge.above();
            if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                continue;
            }
            double top = ledge.getY() + state.getCollisionShape(level, ledge).max(Direction.Axis.Y);
            double height = top - feet;
            if (height < CATCH_LOW || height > CATCH_HIGH) {
                continue;
            }

            // Where the cat hangs: against the wall face, below the ledge top.
            double x = player.getX();
            double z = player.getZ();
            if (facing.getAxis() == Direction.Axis.X) {
                x = facing == Direction.EAST ? ledge.getX() - HALF_WIDTH - WALL_GAP
                        : ledge.getX() + 1.0D + HALF_WIDTH + WALL_GAP;
                z = Mth.clamp(z, ledge.getZ() + 0.2D, ledge.getZ() + 0.8D);
            } else {
                z = facing == Direction.SOUTH ? ledge.getZ() - HALF_WIDTH - WALL_GAP
                        : ledge.getZ() + 1.0D + HALF_WIDTH + WALL_GAP;
                x = Mth.clamp(x, ledge.getX() + 0.2D, ledge.getX() + 0.8D);
            }
            Vec3 anchor = new Vec3(x, top - HANG_BELOW, z);
            Vec3 dest = new Vec3(ledge.getX() + 0.5D, top + 0.002D, ledge.getZ() + 0.5D);
            if (!level.noCollision(player, catBox(anchor))) {
                continue;
            }

            hang = new Hang(facing, ledge, top, anchor, dest);
            player.setPos(anchor);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
            player.setSprinting(false);
            tell(player, NekoLeap.HANG, facing.get2DDataValue());
            return;
        }
    }

    private static AABB catBox(Vec3 feet) {
        return new AABB(feet.x - HALF_WIDTH, feet.y, feet.z - HALF_WIDTH,
                feet.x + HALF_WIDTH, feet.y + CatBody.HEIGHT, feet.z + HALF_WIDTH);
    }

    /** Starts the action here and tells the server, which tells the others. */
    private static void tell(LocalPlayer player, NekoLeap leap, int extra) {
        LeapActions.start(player, leap, extra);
        NekoNetwork.CHANNEL.sendToServer(new LeapPacket(leap.ordinal(), extra));
    }

    private static void release(LocalPlayer player) {
        hang = null;
        tell(player, NekoLeap.NONE, 0);
    }

    // ---- hanging and climbing ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.side != LogicalSide.CLIENT) {
            return;
        }
        Player eventPlayer = event.player;
        if (event.phase == TickEvent.Phase.END) {
            // Everybody who hangs or climbs (also the other players) faces the wall.
            NekoLeap leap = LeapActions.current(eventPlayer);
            if ((leap == NekoLeap.HANG || leap == NekoLeap.CLIMB) && NekoStates.get(eventPlayer).cat()) {
                float yaw = Direction.from2DDataValue(LeapActions.extra(eventPlayer)).toYRot();
                eventPlayer.yBodyRot = yaw;
                eventPlayer.yBodyRotO = yaw;
            }
            return;
        }
        if (!(eventPlayer instanceof LocalPlayer player) || player != minecraft.player || hang == null) {
            return;
        }

        BlockState ledgeState = player.level().getBlockState(hang.ledge);
        if (!NekoStates.get(player).cat() || player.isPassenger() || player.isInWater() || !player.isAlive()
                || !ledgeState.isFaceSturdy(player.level(), hang.ledge, Direction.UP)) {
            release(player);
            return;
        }
        hang.age++;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setSprinting(false);

        if (hang.climbTick < 0) {
            player.setPos(hang.anchor);
            return;
        }

        hang.climbTick++;
        double progress = Math.min(hang.climbTick / (double) CLIMB_TICKS, 1.0D);
        double up = smooth(Math.min(progress / CLIMB_UP_PART, 1.0D));
        double forward = smooth(Math.max((progress - CLIMB_UP_PART) / (1.0D - CLIMB_UP_PART), 0.0D));
        double x = hang.anchor.x + (hang.dest.x - hang.anchor.x) * forward;
        double z = hang.anchor.z + (hang.dest.z - hang.anchor.z) * forward;
        double y = hang.anchor.y + (hang.dest.y - hang.anchor.y) * up;
        player.setPos(x, y, z);
        if (hang.climbTick >= CLIMB_TICKS) {
            release(player);
        }
    }

    private static double smooth(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    /** While hanging the player cannot walk; jump pulls up, crouch lets go. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hang == null || event.getEntity() != minecraft.player) {
            return;
        }
        LocalPlayer player = minecraft.player;
        var input = event.getInput();
        boolean jump = input.jumping;
        boolean crouch = input.shiftKeyDown;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;

        if (hang.climbTick >= 0) {
            return;
        }
        if (crouch && hang.age >= 2) {
            release(player);
            input.shiftKeyDown = true;
            return;
        }
        if (!jump) {
            hang.jumpReleased = true;
        } else if (hang.jumpReleased) {
            Vec3 top = new Vec3(hang.anchor.x, hang.dest.y, hang.anchor.z);
            if (player.level().noCollision(player, catBox(top))
                    && player.level().noCollision(player, catBox(hang.dest))) {
                hang.climbTick = 0;
                tell(player, NekoLeap.CLIMB, hang.facing.get2DDataValue());
            }
        }
    }

    // ---- no attacking or placing in the air ----

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && leaping(player) && (event.isAttack() || event.isUseItem())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
