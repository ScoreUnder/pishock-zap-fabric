package moe.score.pishockzap.compat.clothconfig;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public class BetterMultiElementListEntry<T> extends MultiElementListEntry<T> {
    private final Supplier<T> valueSupplier;

    @SuppressWarnings("UnstableApiUsage")
    public BetterMultiElementListEntry(Component categoryName, Supplier<T> valueSupplier, List<AbstractConfigListEntry<?>> entries) {
        super(categoryName, null, entries, true);
        this.valueSupplier = valueSupplier;
    }

    @Override
    public T getValue() {
        return valueSupplier.get();
    }
}
