package me.shedaniel.clothconfig2.impl.builders;

import lombok.Getter;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
@Getter
public abstract class AbstractSliderFieldBuilder<T, A extends AbstractConfigListEntry<?>, SELF extends FieldBuilder<T, A>> extends FieldBuilder<T, A> {
    protected Function<T, Component> textGetter;
    protected T min;
    protected T max;
    private Consumer<T> saveConsumer = null;
    private Function<T, Optional<Component[]>> tooltipSupplier = (list) -> Optional.empty();
    protected T value;

    protected AbstractSliderFieldBuilder(Component resetButtonKey, Component fieldNameKey) {
        super(resetButtonKey, fieldNameKey);
    }

    public SELF setTextGetter(Function<T, Component> textGetter) {
        this.textGetter = textGetter;
        return (SELF) this;
    }

    protected A finishBuilding(A entry) {
        return entry;
    }

    public SELF setTooltip(Component... tooltip) {
        Optional<Component[]> optional = Optional.ofNullable(tooltip);
        this.tooltipSupplier = v -> optional;
        return (SELF) this;
    }

    public SELF setSaveConsumer(Consumer<T> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return (SELF) this;
    }

    public SELF setDefaultValue(T defaultValue) {
        this.defaultValue = () -> defaultValue;
        return (SELF) this;
    }
}
