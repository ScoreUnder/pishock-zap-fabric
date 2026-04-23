package moe.score.pishockzap.compat.mixin.clothconfig;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import moe.score.pishockzap.compat.mixin.pool.HasDisplayRequirement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(SubCategoryListEntry.class)
@Environment(EnvType.CLIENT)
@SuppressWarnings({"UnstableApiUsage", "rawtypes", "unchecked"})
public abstract class SubCategoryMixin extends TooltipListEntry<List<AbstractConfigListEntry>> implements HasDisplayRequirement {
    @Shadow
    @Final
    private List<AbstractConfigListEntry> entries;
    @Unique
    private BooleanSupplier pishockzap$displayRequirement = () -> true;

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    private SubCategoryMixin(Component fieldName, @Nullable Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
    }

    @Inject(method = "getMorePossibleHeight", at = @At("HEAD"), cancellable = true)
    private void getMorePossibleHeightIfNotDisplayed(CallbackInfoReturnable<Integer> cir) {
        if (!pishockzap$isDisplayed()) cir.setReturnValue(-1);
    }

    @Inject(method = "getItemHeight", at = @At("HEAD"), cancellable = true)
    private void getItemHeightIfNotDisplayed(CallbackInfoReturnable<Integer> cir) {
        if (!pishockzap$isDisplayed()) cir.setReturnValue(0);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderIfNotDisplayed(CallbackInfo cir) {
        if (!pishockzap$isDisplayed()) {
            for (AbstractConfigListEntry entry : this.entries) {
                entry.setParent(this.getParent());
                entry.setScreen(this.getConfigScreen());
            }
            cir.cancel();
        }
    }

    @Inject(method = "lateRender", at = @At("HEAD"), cancellable = true)
    private void lateRenderIfNotDisplayed(CallbackInfo cir) {
        if (!pishockzap$isDisplayed()) cir.cancel();
    }

    @Inject(method = "children", at = @At("HEAD"), cancellable = true)
    private void childrenIfNotDisplayed(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
        if (!pishockzap$isDisplayed()) cir.setReturnValue(Collections.emptyList());
    }

    @Inject(method = "narratables", at = @At("HEAD"), cancellable = true)
    private void narratablesIfNotDisplayed(CallbackInfoReturnable<List<? extends NarratableEntry>> cir) {
        if (!pishockzap$isDisplayed()) cir.setReturnValue(Collections.emptyList());
    }

    @Override
    public boolean pishockzap$isDisplayed() {
        return pishockzap$displayRequirement.getAsBoolean();
    }

    @Override
    public void pishockzap$setDisplayRequirement(BooleanSupplier requirement) {
        pishockzap$displayRequirement = requirement;
    }
}
