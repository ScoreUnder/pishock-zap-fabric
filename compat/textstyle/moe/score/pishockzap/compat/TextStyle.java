package moe.score.pishockzap.compat;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.net.URI;

public class TextStyle {
    private TextStyle() {
    }

    public static Style withHoverText(Style style, Component text) {
        return style.withHoverEvent(new HoverEvent.ShowText(text));
    }

    public static Style withUrlOnClick(Style style, String url) {
        return style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
    }

    public static Style withCommandOnClick(Style style, String command) {
        return style.withClickEvent(new ClickEvent.RunCommand(command));
    }
}
