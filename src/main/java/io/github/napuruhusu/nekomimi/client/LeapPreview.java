package io.github.napuruhusu.nekomimi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.napuruhusu.nekomimi.Nekomimi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shows where the long jump would land while it is ready (cat mode, crouching right after a sprint).
 *
 * Every client tick the jump is played through in advance with the game's own movement rules (gravity,
 * air drag, and the collision of the player's box with the blocks), and the result is drawn as a line
 * for the flight and a square where the cat touches down. The distance is shown above the hotbar.
 */
@Mod.EventBusSubscriber(modid = Nekomimi.MODID, value = Dist.CLIENT)
public final class LeapPreview {
    /** Longest flight that is looked at, in ticks (the jump itself lasts about 14). */
    private static final int MAX_TICKS = 60;

    /** The flight to draw, or null when the jump is not ready. */
    private static volatile Prediction current;

    private LeapPreview() {
    }

    /** The result of playing the jump through. */
    private record Prediction(List<Vec3> path, boolean landed, double distance) {
    }

    /** Called every client tick: ready = the long jump would start if the player jumped now. */
    static void update(LocalPlayer player, boolean ready) {
        if (!ready) {
            current = null;
            return;
        }
        Prediction prediction = simulate(player);
        current = prediction;
        if (prediction.landed()) {
            player.displayClientMessage(Component.translatable("message.nekomimi.leap_distance",
                    String.format("%.1f", prediction.distance())), true);
        } else {
            player.displayClientMessage(Component.translatable("message.nekomimi.leap_far"), true);
        }
    }

    static void clear() {
        current = null;
    }

    private static Prediction simulate(LocalPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        double vx = -Math.sin(yaw) * LeapEvents.LONG_SPEED;
        double vy = LeapEvents.LONG_LIFT;
        double vz = Math.cos(yaw) * LeapEvents.LONG_SPEED;

        AABB box = player.getBoundingBox();
        Vec3 start = new Vec3(box.getCenter().x, box.minY, box.getCenter().z);
        List<Vec3> path = new ArrayList<>();
        path.add(start);
        boolean landed = false;

        for (int tick = 0; tick < MAX_TICKS && !landed; tick++) {
            List<VoxelShape> shapes = new ArrayList<>();
            player.level().getBlockCollisions(player, box.expandTowards(vx, vy, vz).inflate(1.0E-7D))
                    .forEach(shapes::add);

            double dy = vy;
            for (VoxelShape shape : shapes) {
                dy = shape.collide(Direction.Axis.Y, box, dy);
            }
            box = box.move(0.0D, dy, 0.0D);
            if (dy != vy) {
                landed = vy < 0.0D;
                vy = 0.0D;
            }

            // Like the game, the larger horizontal move goes first.
            boolean xFirst = Math.abs(vx) >= Math.abs(vz);
            for (int i = 0; i < 2; i++) {
                boolean doX = (i == 0) == xFirst;
                double want = doX ? vx : vz;
                double got = want;
                for (VoxelShape shape : shapes) {
                    got = shape.collide(doX ? Direction.Axis.X : Direction.Axis.Z, box, got);
                }
                box = doX ? box.move(got, 0.0D, 0.0D) : box.move(0.0D, 0.0D, got);
                if (got != want) {
                    if (doX) {
                        vx = 0.0D;
                    } else {
                        vz = 0.0D;
                    }
                }
            }
            path.add(new Vec3(box.getCenter().x, box.minY, box.getCenter().z));

            // Gravity, then the drag of the air.
            vy = (vy - 0.08D) * 0.98D;
            vx *= 0.91D;
            vz *= 0.91D;
        }

        Vec3 end = path.get(path.size() - 1);
        double distance = Math.hypot(end.x - start.x, end.z - start.z);
        return new Prediction(path, landed, distance);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Prediction prediction = current;
        if (prediction == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();

        float r = prediction.landed() ? 0.35F : 1.0F;
        float g = prediction.landed() ? 1.0F : 0.3F;
        float b = prediction.landed() ? 0.45F : 0.3F;

        List<Vec3> path = prediction.path();
        for (int i = 0; i + 1 < path.size(); i++) {
            line(lines, pose, path.get(i).add(0.0D, 0.05D, 0.0D), path.get(i + 1).add(0.0D, 0.05D, 0.0D), r, g, b);
        }
        if (prediction.landed()) {
            Vec3 end = path.get(path.size() - 1);
            square(lines, pose, end, 0.5D, r, g, b);
            square(lines, pose, end, 0.25D, r, g, b);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** A square outline lying on the ground, centered on `c`, with the given half-size. */
    private static void square(VertexConsumer lines, PoseStack.Pose pose, Vec3 c, double half, float r, float g, float b) {
        double y = c.y + 0.03D;
        Vec3 a = new Vec3(c.x - half, y, c.z - half);
        Vec3 bb = new Vec3(c.x + half, y, c.z - half);
        Vec3 cc = new Vec3(c.x + half, y, c.z + half);
        Vec3 d = new Vec3(c.x - half, y, c.z + half);
        line(lines, pose, a, bb, r, g, b);
        line(lines, pose, bb, cc, r, g, b);
        line(lines, pose, cc, d, r, g, b);
        line(lines, pose, d, a, r, g, b);
    }

    private static void line(VertexConsumer lines, PoseStack.Pose pose, Vec3 from, Vec3 to, float r, float g, float b) {
        Vec3 n = to.subtract(from);
        double len = n.length();
        if (len < 1.0E-6D) {
            return;
        }
        float nx = (float) (n.x / len);
        float ny = (float) (n.y / len);
        float nz = (float) (n.z / len);
        lines.vertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z)
                .color(r, g, b, 1.0F).normal(pose.normal(), nx, ny, nz).endVertex();
        lines.vertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z)
                .color(r, g, b, 1.0F).normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
