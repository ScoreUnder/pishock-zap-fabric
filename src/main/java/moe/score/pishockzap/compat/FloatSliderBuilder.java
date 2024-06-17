//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package moe.score.pishockzap.compat;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractSliderFieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class FloatSliderBuilder extends AbstractSliderFieldBuilder<Float, IntegerSliderEntry, FloatSliderBuilder> {
    private static final float FLOAT_SCALE = 1000.0f;
    
    public FloatSliderBuilder(Text resetButtonKey, Text fieldNameKey, float value, float min, float max) {
        super(resetButtonKey, fieldNameKey);
        this.value = value;
        this.max = max;
        this.min = min;
    }

    public @NotNull IntegerSliderEntry build() {
        IntegerSliderEntry entry = new IntegerSliderEntry(this.getFieldNameKey(), Math.round(this.min * FLOAT_SCALE), Math.round(this.max * FLOAT_SCALE), Math.round(this.value * FLOAT_SCALE), this.getResetButtonKey(), getDefaultValueScaled(), getSaveConsumerScaled(), null, this.isRequireRestart());
        Function<Float, Text> textGetter = this.textGetter;
        if (textGetter != null) {
            Function<Integer, Text> textGetterScaled = (value) -> textGetter.apply(value / FLOAT_SCALE);
            entry.setTextGetter(textGetterScaled);
        } else {
            entry.setTextGetter((value) -> Text.of(String.format("%.3f", value / FLOAT_SCALE)));
        }

        entry.setTooltipSupplier(() -> this.getTooltipSupplier().apply(entry.getValue() / FLOAT_SCALE));
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue() / FLOAT_SCALE));
        }

        return entry;
    }

    @Nullable
    private Consumer<Integer> getSaveConsumerScaled() {
        Consumer<Float> saveConsumer = this.getSaveConsumer();
        return saveConsumer == null ? null : (value) -> saveConsumer.accept(value / FLOAT_SCALE);
    }

    @Nullable
    private Supplier<Integer> getDefaultValueScaled() {
        Supplier<Float> defaultValue = this.defaultValue;
        return defaultValue == null ? null : () -> Math.round(defaultValue.get() * FLOAT_SCALE);
    }
}
