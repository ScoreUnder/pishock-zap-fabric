package moe.score.pishockzap.compat;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.net.URI;

public class TextStyle {
    private TextStyle() {
    }

    public static Style setHoverText(Style style, Text text) {
        return style.withHoverEvent(new HoverEvent.ShowText(text));
    }

    public static Style setUrlOnClick(Style style, String url) {
        return style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
    }
}
