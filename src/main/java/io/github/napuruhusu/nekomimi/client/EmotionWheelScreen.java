package io.github.napuruhusu.nekomimi.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.napuruhusu.nekomimi.network.NekoNetwork;
import io.github.napuruhusu.nekomimi.network.SetStatePacket;
import io.github.napuruhusu.nekomimi.state.NekoEmotion;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The pie menu for picking an emotion. Seven slices around a ring and a "normal" button in the middle.
 *
 * Point at a slice and click, or hold the key, point, and let go of the key. A quick tap of the
 * key leaves the menu open so you can click. Escape or right click closes it without a change.
 */
public class EmotionWheelScreen extends Screen {
    private static final NekoEmotion[] SLICES = {
            NekoEmotion.HAPPY, NekoEmotion.RELAXED, NekoEmotion.ANNOYED, NekoEmotion.SCARED,
            NekoEmotion.SURPRISED, NekoEmotion.INTERESTED, NekoEmotion.AFFECTIONATE};

    private static final int NOTHING = -1;
    private static final int CENTER = SLICES.length;

    private static final float INNER_RADIUS = 34.0F;
    private static final float OUTER_RADIUS = 104.0F;
    private static final float SLICE_GAP = 0.035F;
    /** Letting go of the key within this time counts as a tap, not as a choice. */
    private static final long TAP_MILLIS = 250L;

    private static final int COLOR_SLICE = 0xB0202028;
    private static final int COLOR_HOVER = 0xE0F0A040;
    private static final int COLOR_CURRENT = 0xC04080C0;

    private final NekoEmotion current;
    private final long openedAt = Util.getMillis();
    private int hovered = NOTHING;

    public EmotionWheelScreen(NekoEmotion current) {
        super(Component.translatable("nekomimi.emotion.title"));
        this.current = current;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        float cx = this.width / 2.0F;
        float cy = this.height / 2.0F;
        hovered = pick(mouseX - cx, mouseY - cy);

        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float step = Mth.TWO_PI / SLICES.length;
        for (int i = 0; i < SLICES.length; i++) {
            float middle = sliceAngle(i);
            int color = i == hovered ? COLOR_HOVER : SLICES[i] == current ? COLOR_CURRENT : COLOR_SLICE;
            fillRing(matrix, cx, cy, INNER_RADIUS + 3.0F, OUTER_RADIUS,
                    middle - step / 2.0F + SLICE_GAP, middle + step / 2.0F - SLICE_GAP, color);
        }
        int centerColor = hovered == CENTER ? COLOR_HOVER : current == NekoEmotion.NONE ? COLOR_CURRENT : COLOR_SLICE;
        fillRing(matrix, cx, cy, 0.0F, INNER_RADIUS, 0.0F, Mth.TWO_PI, centerColor);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        for (int i = 0; i < SLICES.length; i++) {
            float middle = sliceAngle(i);
            float radius = (INNER_RADIUS + OUTER_RADIUS) / 2.0F + 4.0F;
            graphics.drawCenteredString(this.font, Component.translatable(SLICES[i].translationKey()),
                    Math.round(cx + Mth.cos(middle) * radius), Math.round(cy + Mth.sin(middle) * radius) - 4, 0xFFFFFF);
        }
        graphics.drawCenteredString(this.font, Component.translatable(NekoEmotion.NONE.translationKey()),
                Math.round(cx), Math.round(cy) - 4, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("nekomimi.emotion.title"),
                Math.round(cx), Math.round(cy - OUTER_RADIUS) - 16, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Angle of the middle of a slice. Slice 0 is at the top and they go clockwise. */
    private static float sliceAngle(int index) {
        return -Mth.HALF_PI + index * Mth.TWO_PI / SLICES.length;
    }

    /** Which slice (or the middle button) is under the pointer, given its offset from the center. */
    private static int pick(float dx, float dy) {
        float distance = Mth.sqrt(dx * dx + dy * dy);
        if (distance < INNER_RADIUS) {
            return CENTER;
        }
        if (distance > OUTER_RADIUS + 24.0F) {
            return NOTHING;
        }
        float step = Mth.TWO_PI / SLICES.length;
        float angle = (float) Math.atan2(dy, dx) + Mth.HALF_PI + step / 2.0F;
        angle = angle - Mth.TWO_PI * (float) Math.floor(angle / Mth.TWO_PI);
        return Math.min((int) (angle / step), SLICES.length - 1);
    }

    /** Fills the part of a ring between two radii and two angles. Radius 0 to r is a disc. */
    private static void fillRing(Matrix4f matrix, float cx, float cy, float inner, float outer,
                                 float from, float to, int argb) {
        int alpha = argb >>> 24;
        int red = argb >> 16 & 255;
        int green = argb >> 8 & 255;
        int blue = argb & 255;
        int segments = Math.max(2, (int) Math.ceil((to - from) / 0.1F));

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = from + (to - from) * i / segments;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            builder.vertex(matrix, cx + cos * inner, cy + sin * inner, 0.0F).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, cx + cos * outer, cy + sin * outer, 0.0F).color(red, green, blue, alpha).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered != NOTHING) {
            choose(hovered);
            return true;
        }
        if (button == 1) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Held and let go on a slice: that is the choice.
        if (ModKeys.EMOTION_MENU.matches(keyCode, scanCode)
                && Util.getMillis() - openedAt > TAP_MILLIS && hovered != NOTHING) {
            choose(hovered);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void choose(int index) {
        NekoEmotion emotion = index == CENTER ? NekoEmotion.NONE : SLICES[index];
        NekoNetwork.CHANNEL.sendToServer(new SetStatePacket(true, emotion.ordinal()));
        this.onClose();
    }
}
