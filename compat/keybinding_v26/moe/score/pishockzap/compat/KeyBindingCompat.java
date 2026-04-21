package moe.score.pishockzap.compat;

import com.mojang.blaze3d.platform.InputConstants;
import moe.score.pishockzap.PishockZapMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBindingCompat {
    public static KeyMapping registerKeyBinding(String id, InputConstants.Type type, int code, String path) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            id,
            type,
            code,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PishockZapMod.ID, path))
        ));
    }
}
