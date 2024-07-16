//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package moe.score.pishockzap.compat;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.NonNull;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractSliderFieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class FloatSliderBuilder extends AbstractSliderFieldBuilder<Float, IntegerSliderEntry, FloatSliderBuilder> {
    private final float floatScale;

    public FloatSliderBuilder(Text resetButtonKey, Text fieldNameKey, float value, float min, float max, float floatScale) {
        super(resetButtonKey, fieldNameKey);
        this.value = value;
        this.max = max;
        this.min = min;
        this.floatScale = floatScale;
    }

    public FloatSliderBuilder(Text resetButtonKey, Text fieldNameKey, float value, float min, float max) {
        this(resetButtonKey, fieldNameKey, value, min, max, 1000.0f);
    }

    public @NonNull IntegerSliderEntry build() {
        IntegerSliderEntry entry = new IntegerSliderEntry(this.getFieldNameKey(), Math.round(this.min * floatScale), Math.round(this.max * floatScale), Math.round(this.value * floatScale), this.getResetButtonKey(), getDefaultValueScaled(), getSaveConsumerScaled(), null, this.isRequireRestart());
        Function<Float, Text> textGetter = this.textGetter;
        if (textGetter != null) {
            Function<Integer, Text> textGetterScaled = (value) -> textGetter.apply(value / floatScale);
            entry.setTextGetter(textGetterScaled);
        } else {
            entry.setTextGetter((value) -> Text.of(String.format("%.3f", value / floatScale)));
        }

        entry.setTooltipSupplier(() -> this.getTooltipSupplier().apply(entry.getValue() / floatScale));
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue() / floatScale));
        }

        return this.finishBuilding(entry);
    }

    @Nullable
    private Consumer<Integer> getSaveConsumerScaled() {
        Consumer<Float> saveConsumer = this.getSaveConsumer();
        return saveConsumer == null ? null : (value) -> saveConsumer.accept(value / floatScale);
    }

    @Nullable
    private Supplier<Integer> getDefaultValueScaled() {
        Supplier<Float> defaultValue = this.defaultValue;
        return defaultValue == null ? null : () -> Math.round(defaultValue.get() * floatScale);
    }
}
