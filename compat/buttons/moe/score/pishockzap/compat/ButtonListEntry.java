package moe.score.pishockzap.compat;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.Window;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ExtensionMethod(WidgetUtil.Extensions.class)
public class ButtonListEntry extends TooltipListEntry<Void> {
    private final Button buttonWidget;
    private final List<AbstractWidget> widgets;

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    @Builder(setterPrefix = "set")
    public ButtonListEntry(Component fieldName, Component buttonText, Consumer<ButtonListEntry> onClickCallback, Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        this.buttonWidget = WidgetUtil.makeButton(0, 0, 150, 20, buttonText, btn -> onClickCallback.accept(this));
        this.widgets = ImmutableList.of(buttonWidget);
    }

    public void setButtonText(Component text) {
        buttonWidget.setMessage(text);
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public void save() {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = Minecraft.getInstance().getWindow();
        buttonWidget.active = isEditable();
        buttonWidget.setY(y);
        var displayed = getDisplayedFieldName();
        var textRenderer = Minecraft.getInstance().font;
        if (textRenderer.isBidirectional()) {
            graphics.text(textRenderer, displayed.getVisualOrderText(), window.getGuiScaledWidth() - x - textRenderer.width(displayed), y + 6, 0xffffff);
            buttonWidget.setX(x);
        } else {
            graphics.text(textRenderer, displayed.getVisualOrderText(), x, y + 6, this.getPreferredTextColor());
            buttonWidget.setX(x + entryWidth - 150);
        }

        buttonWidget.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        return widgets;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return widgets;
    }
}
