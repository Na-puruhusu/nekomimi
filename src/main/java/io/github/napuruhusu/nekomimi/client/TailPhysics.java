package io.github.napuruhusu.nekomimi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps one {@link TailSimulation} per wearer and feeds it how that wearer is moving.
 * Client side and visual only: nothing here is sent to the server or changes the game.
 *
 * The motion is measured from the entity's interpolated position between two renders rather than
 * from its movement vector, so it works the same for the local player, other players, and mobs.
 */
public final class TailPhysics {
    private static final float SECONDS_PER_TICK = 0.05F;
    /** If an entity was not drawn for this long (in ticks), its tail starts again from rest. */
    private static final float MAX_GAP_TICKS = 10.0F;
    /** Moving farther than this between two renders counts as a teleport. */
    private static final double TELEPORT_DISTANCE_SQR = 8.0D * 8.0D;
    /** Frames closer together than this are the same frame drawn twice (shadows, shaders...). */
    private static final float MIN_STEP_TICKS = 1.0E-3F;

    // Weak keys: an entity that is gone from the world frees its tracker.
    private static final Map<LivingEntity, Tracker> TRACKERS = new WeakHashMap<>();

    private TailPhysics() {
    }

    private static final class Tracker {
        final TailSimulation simulation = new TailSimulation();
        boolean started;
        float lastTime;
        double lastX;
        double lastY;
        double lastZ;
        float lastYaw;
    }

    /** Brings the wearer's tail up to date for this frame and returns it. Safe to call several times per frame. */
    public static TailSimulation update(LivingEntity entity) {
        Tracker tracker = TRACKERS.computeIfAbsent(entity, e -> new Tracker());

        float partial = Minecraft.getInstance().getFrameTime();
        float now = entity.tickCount + partial;
        double x = Mth.lerp((double) partial, entity.xo, entity.getX());
        double y = Mth.lerp((double) partial, entity.yo, entity.getY());
        double z = Mth.lerp((double) partial, entity.zo, entity.getZ());
        float yaw = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);

        float gap = now - tracker.lastTime;
        double dx = x - tracker.lastX;
        double dy = y - tracker.lastY;
        double dz = z - tracker.lastZ;

        if (!tracker.started || gap < 0.0F || gap > MAX_GAP_TICKS
                || dx * dx + dy * dy + dz * dz > TELEPORT_DISTANCE_SQR) {
            tracker.simulation.reset();
        } else if (gap >= MIN_STEP_TICKS) {
            float dt = gap * SECONDS_PER_TICK;
            double yawRad = Math.toRadians(yaw);
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            // The entity faces (-sin, cos); its left is (cos, sin).
            float forward = (float) ((-dx * sin + dz * cos) / dt);
            float left = (float) ((dx * cos + dz * sin) / dt);
            float up = (float) (dy / dt);
            float yawRate = (float) Math.toRadians(Mth.wrapDegrees(yaw - tracker.lastYaw)) / dt;
            tracker.simulation.step(dt, forward, left, up, yawRate);
        } else {
            // Same frame again: keep the current pose and the previous sample.
            return tracker.simulation;
        }

        tracker.started = true;
        tracker.lastTime = now;
        tracker.lastX = x;
        tracker.lastY = y;
        tracker.lastZ = z;
        tracker.lastYaw = yaw;
        return tracker.simulation;
    }
}
