package io.github.napuruhusu.nekomimi.client;

import io.github.napuruhusu.nekomimi.state.NekoLeap;
import net.minecraft.world.entity.player.Player;

import java.util.WeakHashMap;

/**
 * The boosted jump each player is in right now, for drawing the motion. It starts when the jump is
 * reported (by the player's own client, or by the server for others) and ends when the player lands.
 * Client only.
 */
public final class LeapActions {
    /** The player is still on the ground in the first ticks after the jump starts, so landing is not checked then. */
    private static final int MIN_TICKS = 3;
    /** A leap never lasts this long; this ends one that was never followed by a landing (a flight, a teleport). */
    private static final int MAX_TICKS = 120;

    private record Entry(NekoLeap leap, int startTick) {
    }

    private static final WeakHashMap<Player, Entry> ACTIVE = new WeakHashMap<>();

    private LeapActions() {
    }

    public static void start(Player player, NekoLeap leap) {
        if (leap == NekoLeap.NONE) {
            ACTIVE.remove(player);
        } else {
            ACTIVE.put(player, new Entry(leap, player.tickCount));
        }
    }

    /** The leap the player is in now, or NONE. */
    public static NekoLeap current(Player player) {
        Entry entry = ACTIVE.get(player);
        if (entry == null) {
            return NekoLeap.NONE;
        }
        int age = player.tickCount - entry.startTick();
        if (age < 0 || age > MAX_TICKS || (age >= MIN_TICKS && (player.onGround() || player.isInWater()))) {
            ACTIVE.remove(player);
            return NekoLeap.NONE;
        }
        return entry.leap();
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
