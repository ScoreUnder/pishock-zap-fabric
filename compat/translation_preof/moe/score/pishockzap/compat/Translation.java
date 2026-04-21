package moe.score.pishockzap.compat;

import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public final class Translation {
    private Translation() {
    }

    public static @NonNull TranslatableComponent of(@NonNull String key) {
        return new TranslatableComponent(key);
    }

    public static @NonNull TranslatableComponent of(@NonNull String key, Object... args) {
        return new TranslatableComponent(key, args);
    }

    public static @NonNull MutableComponent raw(@NonNull String text) {
        return new TextComponent(text);
    }

    public static @NonNull MutableComponent addLink(@NonNull MutableComponent text, @NonNull String url) {
        return addLink(text, url, raw(url));
    }

    public static @NonNull MutableComponent addLink(@NonNull MutableComponent text, @NonNull String url, @NonNull Component tooltip) {
        return text.withStyle(style ->
            TextStyle.setHoverText(TextStyle.setUrlOnClick(style, url), tooltip)
                .withUnderlined(true)
                .withColor(ChatFormatting.BLUE)
        );
    }
}
