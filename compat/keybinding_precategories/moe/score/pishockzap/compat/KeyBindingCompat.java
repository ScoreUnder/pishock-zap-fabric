package moe.score.pishockzap.compat;

import com.mojang.blaze3d.platform.InputConstants;
import moe.score.pishockzap.Constants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class KeyBindingCompat {
    public static KeyMapping registerKeyBinding(String id, InputConstants.Type type, int code, String path) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            id,
            type,
            code,
            "key.category." + Constants.ID + "." + path));
    }
}
