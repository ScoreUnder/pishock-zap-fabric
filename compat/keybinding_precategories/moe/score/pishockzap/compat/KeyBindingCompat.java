package moe.score.pishockzap.compat;

import com.mojang.blaze3d.platform.InputConstants;
import moe.score.pishockzap.Constants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindingCompat {
    public static final int KEY_F12 = GLFW.GLFW_KEY_F12;

    public static KeyMapping registerKeyBinding(String id, int code, String path) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            id,
            InputConstants.Type.KEYSYM,
            code,
            "key.category." + Constants.ID + "." + path));
    }
}
