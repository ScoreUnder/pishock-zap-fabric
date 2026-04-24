package moe.score.pishockzap.compat.clothconfig;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class Arity2StructEntry<A, B, T> extends MultiElementListEntry<T> {
    private final Supplier<? extends T> valueSupplier;

    @SuppressWarnings("UnstableApiUsage")
    public Arity2StructEntry(Component categoryName, BiFunction<? super A, ? super B, ? extends T> valueCreator, AbstractConfigListEntry<? extends A> aEntry, AbstractConfigListEntry<? extends B> bEntry) {
        super(categoryName, null, List.of(aEntry, bEntry), true);
        this.valueSupplier = () -> valueCreator.apply(aEntry.getValue(), bEntry.getValue());
    }

    @Override
    public T getValue() {
        return valueSupplier.get();
    }
}
