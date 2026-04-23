package moe.score.pishockzap.compat;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public class TextStyle {
    private TextStyle() {
    }

    public static Style withHoverText(Style style, Component text) {
        return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
    }

    public static Style withUrlOnClick(Style style, String url) {
        return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
    }
}
