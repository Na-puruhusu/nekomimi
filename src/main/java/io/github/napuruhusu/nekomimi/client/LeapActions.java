package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoLeap;
import net.minecraft.world.entity.player.Player;

import java.util.WeakHashMap;

/**
 * The boosted jump, ledge hang or climb each player is in right now, for drawing the motion. It starts when
 * it is reported (by the player's own client, or by the server for others). A jump ends when the player
 * lands; a hang or climb ends when it is reported ended (or, as a safety net, after a long time). Client only.
 */
public final class LeapActions {
    /** The player is still on the ground in the first ticks after the jump starts, so landing is not checked then. */
    private static final int MIN_TICKS = 3;
    /** A leap never lasts this long; this ends one that was never followed by a landing (a flight, a teleport). */
    private static final int MAX_TICKS = 120;
    /** A hang ends when the cat lets go, but nobody hangs forever (10 minutes). */
    private static final int MAX_HANG_TICKS = 12000;
    private static final int MAX_CLIMB_TICKS = 30;

    private record Entry(NekoLeap leap, int startTick, int extra) {
    }

    private static final WeakHashMap<Player, Entry> ACTIVE = new WeakHashMap<>();

    private LeapActions() {
    }

    public static void start(Player player, NekoLeap leap) {
        start(player, leap, 0);
    }

    /** `extra` is the side (0-3, Direction.get2DDataValue) the cat faces when hanging or climbing. */
    public static void start(Player player, NekoLeap leap, int extra) {
        if (leap == NekoLeap.NONE) {
            ACTIVE.remove(player);
        } else {
            ACTIVE.put(player, new Entry(leap, player.tickCount, extra));
        }
    }

    /** The side the cat faces in its hang or climb. */
    public static int extra(Player player) {
        Entry entry = ACTIVE.get(player);
        return entry == null ? 0 : entry.extra();
    }

    /** How long the current action has lasted, in ticks (with the fraction of the tick given by `ageInTicks`). */
    public static float age(Player player, float ageInTicks) {
        Entry entry = ACTIVE.get(player);
        return entry == null ? 0.0F : ageInTicks - entry.startTick();
    }

    /** The leap the player is in now, or NONE. */
    public static NekoLeap current(Player player) {
        Entry entry = ACTIVE.get(player);
        if (entry == null) {
            return NekoLeap.NONE;
        }
        int age = player.tickCount - entry.startTick();
        boolean over = switch (entry.leap()) {
            case HANG -> age > MAX_HANG_TICKS;
            case CLIMB -> age > MAX_CLIMB_TICKS;
            default -> age > MAX_TICKS || (age >= MIN_TICKS && (player.onGround() || player.isInWater()));
        };
        if (age < 0 || over) {
            ACTIVE.remove(player);
            return NekoLeap.NONE;
        }
        return entry.leap();
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
