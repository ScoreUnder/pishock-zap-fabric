package moe.score.pishockzap.mixin.pool;

public interface ListEntryExt {
    void pishockZap$addListEntry(Object value);

    void pishockZap$addListEntries(Iterable<?> values);
}
