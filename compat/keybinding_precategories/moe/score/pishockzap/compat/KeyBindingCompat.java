package moe.score.pishockzap.compat;

import moe.score.pishockzap.PishockZapMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class KeyBindingCompat {
    public static KeyBinding registerKeyBinding(String id, InputUtil.Type type, int code, String path) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
            id,
            type,
            code,
            "key.category." + PishockZapMod.ID + "." + path));
    }
}
