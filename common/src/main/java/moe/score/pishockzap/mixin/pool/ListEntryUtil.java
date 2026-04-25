package moe.score.pishockzap.mixin.pool;

import lombok.experimental.UtilityClass;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
@UtilityClass
@ApiStatus.Internal
public class ListEntryUtil {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> void withExtensions(AbstractListListEntry<T, ?, ?> entry, Consumer<ListEntryExt<T>> consumer) {
         if (entry instanceof ListEntryExt ext) consumer.accept(ext);
    }

    public static <T> void replaceValues(ListEntryExt<T> entry, Iterable<? extends T> values) {
        entry.pishockZap$clear();
        entry.pishockZap$addListEntries(values);
    }
}
