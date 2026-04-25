package moe.score.pishockzap.compat;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

@UtilityClass
public class WidgetUtil {
    Button makeButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress).bounds(x, y, width, height).build();
    }

    @UtilityClass
    public static class Extensions {}
}
