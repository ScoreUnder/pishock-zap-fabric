//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package moe.score.pishockzap.compat;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
@Accessors(chain = true)
public class FloatSliderBuilder extends FieldBuilder<Float, IntegerSliderEntry> {
    private final float floatScale;
    private final float value;
    private float max;
    private float min;
    @Getter
    @Setter
    private Consumer<Float> saveConsumer = null;
    @Getter
    @Setter
    private Function<Float, Optional<Component[]>> tooltipSupplier = _i -> Optional.empty();
    @Getter
    @Setter
    private Function<Float, Component> textGetter = null;

    public FloatSliderBuilder(@NonNull Component resetButtonKey, @NonNull Component fieldNameKey, float value, float min, float max, float floatScale) {
        super(resetButtonKey, fieldNameKey);
        this.value = value;
        this.max = max;
        this.min = min;
        this.floatScale = floatScale;
    }

    public FloatSliderBuilder(@NonNull Component resetButtonKey, @NonNull Component fieldNameKey, float value, float min, float max) {
        this(resetButtonKey, fieldNameKey, value, min, max, 1000.0f);
    }

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    public @NonNull IntegerSliderEntry build() {
        IntegerSliderEntry entry = new IntegerSliderEntry(this.getFieldNameKey(), Math.round(this.min * floatScale), Math.round(this.max * floatScale), Math.round(this.value * floatScale), this.getResetButtonKey(), getDefaultValueScaled(), getSaveConsumerScaled(), null, this.isRequireRestart());
        Function<Float, Component> textGetter = this.textGetter;
        if (textGetter != null) {
            Function<Integer, Component> textGetterScaled = (value) -> textGetter.apply(value / floatScale);
            entry.setTextGetter(textGetterScaled);
        } else {
            entry.setTextGetter((value) -> Translation.raw(String.format("%.3f", value / floatScale)));
        }

        entry.setTooltipSupplier(() -> this.getTooltipSupplier().apply(entry.getValue() / floatScale));
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue() / floatScale));
        }

        return entry;
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

    public FloatSliderBuilder setTooltip(Component... tooltips) {
        setTooltipSupplier(_f -> Optional.of(tooltips));
        return this;
    }

    public FloatSliderBuilder setDefaultValue(Float defaultValue) {
        this.defaultValue = () -> defaultValue;
        return this;
    }
}
