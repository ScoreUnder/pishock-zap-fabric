package moe.score.pishockzap.compat;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public final class Translation {
    private Translation() {
    }

    public static TranslatableText of(String key) {
        return new TranslatableText(key);
    }

    public static TranslatableText of(String key, Object... args) {
        return new TranslatableText(key, args);
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
