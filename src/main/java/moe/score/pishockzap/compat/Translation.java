package moe.score.pishockzap.compat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class Translation {
    private Translation() {
    }

    public static MutableText of(String key) {
        return Text.translatable(key);
    }
}
