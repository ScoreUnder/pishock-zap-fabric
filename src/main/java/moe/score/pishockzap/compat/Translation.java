package moe.score.pishockzap.compat;

import lombok.NonNull;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;

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
            style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                .withHoverEvent(new HoverEvent.ShowText(tooltip))
                .withUnderline(true)
                .withColor(Formatting.BLUE)
        );
    }
}
