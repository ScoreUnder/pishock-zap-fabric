package moe.score.pishockzap.compat;

import lombok.NonNull;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public final class Translation {
    private Translation() {
    }

    public static @NonNull MutableText of(@NonNull String key) {
        return Text.translatable(key);
    }

    public static @NonNull MutableText of(@NonNull String key, Object... args) {
        return Text.translatable(key, args);
    }

    public static @NonNull MutableText raw(@NonNull String text) {
        return Text.literal(text);
    }

    public static @NonNull MutableText addLink(@NonNull MutableText text, @NonNull String url) {
        return addLink(text, url, Text.of(url));
    }

    public static @NonNull MutableText addLink(@NonNull MutableText text, @NonNull String url, @NonNull Text tooltip) {
        return text.styled(style ->
            style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))
                .withUnderline(true)
                .withColor(Formatting.BLUE)
        );
    }
}
