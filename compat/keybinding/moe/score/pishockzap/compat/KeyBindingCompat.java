package moe.score.pishockzap.compat;

import com.mojang.blaze3d.platform.InputConstants;
import moe.score.pishockzap.PishockZapMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public class KeyBindingCompat {
    public static KeyMapping registerKeyBinding(String id, InputConstants.Type type, int code, String path) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
            id,
            type,
            code,
            KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(PishockZapMod.ID, path))
        ));
    }
}
