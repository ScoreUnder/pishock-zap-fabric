package moe.score.pishockzap.mixin.pool;

import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import org.jetbrains.annotations.Nullable;

public interface ListEntryExt<T> {
    void pishockZap$addListEntry(T value);

    void pishockZap$addListEntries(Iterable<? extends T> values);

    @SuppressWarnings({"rawtypes", "UnstableApiUsage", "unchecked"})
    @Nullable
    static <T> ListEntryExt<T> of(AbstractListListEntry<T, ?, ?> entry) {
        if (entry instanceof ListEntryExt ext) return ext;
        return null;
    }
}
