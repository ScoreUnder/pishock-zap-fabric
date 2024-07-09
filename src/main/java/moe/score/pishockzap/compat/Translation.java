package moe.score.pishockzap.compat;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public final class Translation {
    private Translation() {
    }

    public static MutableText of(String key) {
        return Text.translatable(key);
    }

    public static MutableText of(String key, Object... args) {
        return Text.translatable(key, args);
    }

    public static MutableText raw(String text) {
        return new LiteralText(text);
    }

    public static MutableText addLink(MutableText text, String url) {
        return addLink(text, url, Text.of(url));
    }

    public static MutableText addLink(MutableText text, String url, Text tooltip) {
        return text.styled(style ->
            style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))
                .withUnderline(true)
                .withColor(Formatting.BLUE)
        );
    }
}
