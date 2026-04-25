package moe.score.pishockzap.compat;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

@UtilityClass
public class WidgetUtil {
    public static Button makeButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return new Button(x, y, width, height, text, onPress);
    }

    @UtilityClass
    public static class Extensions {
        public static void setX(AbstractWidget w, int x) {
            w.x = x;
        }

        public static void setY(AbstractWidget w, int y) {
            w.y = y;
        }
    }
}
