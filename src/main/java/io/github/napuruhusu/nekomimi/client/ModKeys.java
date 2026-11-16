package io.github.napuruhusu.nekomimi.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Key bindings. All can be changed in the controls screen. Alt + right click also switches the mode. */
public final class ModKeys {
    public static final String CATEGORY = "key.categories.nekomimi";

    /** Opens the emotion menu (cat mode). */
    public static final KeyMapping EMOTION_MENU = new KeyMapping("key.nekomimi.emotion_menu",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    /** Opens the pose menu (cat mode). */
    public static final KeyMapping POSE_MENU = new KeyMapping("key.nekomimi.pose_menu",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    /** Switches between normal and cat mode. Unbound by default, because Alt + right click does it. */
    public static final KeyMapping TOGGLE_MODE = new KeyMapping("key.nekomimi.toggle_mode",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);

    private ModKeys() {
    }
}
