package moe.score.pishockzap.compat.clothconfig;

import lombok.Builder;
import lombok.experimental.UtilityClass;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@UtilityClass
@SuppressWarnings("UnstableApiUsage")
public class NestedList {
    @Builder(setterPrefix = "set")
    public static <T, W extends AbstractConfigListEntry<T>> NestedListListEntry<T, W> create(
        Component title,
        List<T> initialValue,
        Supplier<List<T>> defaultValueSupplier,
        T defaultNewEntryValue,
        Consumer<List<T>> saveConsumer,
        Component resetButtonKey,
        BiFunction<? super T, ? super NestedListListEntry<T, W>, ? extends W> widgetCreator,
        Supplier<Optional<Component[]>> tooltipSupplier
    ) {
        return new NestedListListEntry<>(
            title,
            initialValue,
            true,
            tooltipSupplier,
            saveConsumer,
            defaultValueSupplier,
            resetButtonKey,
            true,
            true,
            (elem, self) -> widgetCreator.apply(elem == null ? defaultNewEntryValue : elem, self)
        );
    }
}
