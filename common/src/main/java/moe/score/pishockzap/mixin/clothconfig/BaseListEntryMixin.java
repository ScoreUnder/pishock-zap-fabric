package moe.score.pishockzap.mixin.clothconfig;

import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import moe.score.pishockzap.mixin.pool.ListEntryExt;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"UnstableApiUsage", "unchecked", "rawtypes"})
@Mixin(AbstractListListEntry.class)
public abstract class BaseListEntryMixin extends BaseListEntry implements ListEntryExt {
    @Unique
    private BiFunction pishockZap$createNewListEntry;

    private BaseListEntryMixin(@NotNull Component fieldName, @Nullable Supplier tooltipSupplier, @Nullable Supplier defaultValue, @NotNull Function createNewInstance, @Nullable Consumer saveConsumer, Component resetButtonKey) {
        super(fieldName, tooltipSupplier, defaultValue, createNewInstance, saveConsumer, resetButtonKey);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void saveSuperConstructor(Component fieldName, List value, boolean defaultExpanded, Supplier tooltipSupplier, Consumer saveConsumer, Supplier defaultValue, Component resetButtonKey, boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront, BiFunction createNewCell, CallbackInfo ci) {
        pishockZap$createNewListEntry = createNewCell;
    }

    @Override
    public void pishockZap$addListEntry(Object value) {
        // please i cannot believe there is not already an API for this
        var listCell = pishockZap$createNewListEntry.apply(value, this);
        this.cells.add(listCell);
        this.widgets.add(listCell);
    }

    @Override
    public void pishockZap$addListEntries(Iterable values) {
        for (var v : values)  {
            pishockZap$addListEntry(v);
        }
    }
}
