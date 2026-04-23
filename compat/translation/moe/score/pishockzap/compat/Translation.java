package moe.score.pishockzap.compat;

import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Translation {
    private Translation() {
    }

    public static @NonNull MutableComponent of(@NonNull String key) {
        return Component.translatable(key);
    }

    public static @NonNull MutableComponent of(@NonNull String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static @NonNull MutableComponent raw(@NonNull String text) {
        return Component.literal(text);
    }

    public static @NonNull MutableComponent addLink(@NonNull MutableComponent text, @NonNull String url) {
        return addLink(text, url, Component.literal(url));
    }

    public static @NonNull MutableComponent addLink(@NonNull MutableComponent text, @NonNull String url, @NonNull Component tooltip) {
        return text.withStyle(style ->
            TextStyle.withHoverText(TextStyle.withUrlOnClick(style, url), tooltip)
                .withUnderlined(true)
                .withColor(ChatFormatting.BLUE)
        );
    }
}
