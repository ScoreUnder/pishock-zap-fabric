package moe.score.pishockzap.compat;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;

public class TextStyle {
    private TextStyle() {
    }

    public static Style setHoverText(Style style, TranslatableText text) {
        return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
    }
}
