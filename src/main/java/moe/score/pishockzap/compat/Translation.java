package moe.score.pishockzap.compat;

import net.minecraft.text.TranslatableText;

public final class Translation {
    private Translation() {
    }

    public static TranslatableText of(String key) {
        return new TranslatableText(key);
    }
}
