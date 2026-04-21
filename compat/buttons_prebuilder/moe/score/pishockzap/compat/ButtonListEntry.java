package moe.score.pishockzap.compat;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Builder;
import lombok.NonNull;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
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
        this.buttonWidget = new Button(0, 0, 150, 20, buttonText, btn -> onClickCallback.accept(this));
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

    public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = Minecraft.getInstance().getWindow();
        buttonWidget.active = isEditable();
        buttonWidget.y = y;
        var displayed = getDisplayedFieldName();
        var textRenderer = Minecraft.getInstance().font;
        if (textRenderer.isBidirectional()) {
            textRenderer.drawShadow(matrices, displayed.getVisualOrderText(), (float) (window.getGuiScaledWidth() - x - textRenderer.width(displayed)), y + 6, 0xffffff);
            buttonWidget.x = x;
        } else {
            textRenderer.drawShadow(matrices, displayed.getVisualOrderText(), (float) x, (float) (y + 6), this.getPreferredTextColor());
            buttonWidget.x = x + entryWidth - 150;
        }

        buttonWidget.render(matrices, mouseX, mouseY, delta);
    }

    public @NonNull List<? extends GuiEventListener> children() {
        return widgets;
    }

    public List<? extends NarratableEntry> narratables() {
        return widgets;
    }
}
