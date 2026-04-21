package moe.score.pishockzap.compat;

import com.mojang.blaze3d.platform.Window;
import lombok.Builder;
import lombok.NonNull;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonListEntry extends TooltipListEntry<Void> {
    private final Button buttonWidget;
    private final List<AbstractWidget> widgets;

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    @Builder(setterPrefix = "set")
    public ButtonListEntry(Component fieldName, Component buttonText, Consumer<ButtonListEntry> onClickCallback, Supplier<Optional<Component[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        this.buttonWidget = Button.builder(buttonText, btn -> onClickCallback.accept(this))
            .bounds(0, 0, 150, 20).build();
        this.widgets = List.of(buttonWidget);
    }

    public void setButtonText(Component text) {
        buttonWidget.setMessage(text);
    }

    public boolean isEdited() {
        return false;
    }

    public Void getValue() {
        return null;
    }

    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = Minecraft.getInstance().getWindow();
        buttonWidget.active = isEditable();
        buttonWidget.setY(y);
        var displayed = getDisplayedFieldName();
        var textRenderer = Minecraft.getInstance().font;
        if (textRenderer.isBidirectional()) {
            graphics.drawString(textRenderer, displayed.getVisualOrderText(), window.getGuiScaledWidth() - x - textRenderer.width(displayed), y + 6, 0xffffff);
            buttonWidget.setX(x);
        } else {
            graphics.drawString(textRenderer, displayed.getVisualOrderText(), x, y + 6, this.getPreferredTextColor());
            buttonWidget.setX(x + entryWidth - 150);
        }

        buttonWidget.render(graphics, mouseX, mouseY, delta);
    }

    public @NonNull List<? extends GuiEventListener> children() {
        return widgets;
    }

    public List<? extends NarratableEntry> narratables() {
        return widgets;
    }
}
