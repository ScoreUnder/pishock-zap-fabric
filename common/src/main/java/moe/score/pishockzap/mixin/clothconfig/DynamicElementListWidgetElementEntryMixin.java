package moe.score.pishockzap.mixin.clothconfig;

import me.shedaniel.clothconfig2.gui.widget.DynamicElementListWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(DynamicElementListWidget.ElementEntry.class)
public class DynamicElementListWidgetElementEntryMixin {
    @Shadow
    private @Nullable GuiEventListener focused;

    @Inject(method = "setFocused", at = @At("HEAD"), cancellable = true)
    private void fixFocusJank(GuiEventListener focused, CallbackInfo ci) {
        if (focused == this.focused) ci.cancel();
    }
}
