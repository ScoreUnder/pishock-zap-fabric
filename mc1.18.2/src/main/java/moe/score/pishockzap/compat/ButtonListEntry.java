package moe.score.pishockzap.compat;

import lombok.Builder;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonListEntry extends TooltipListEntry<Void> {
    private final ButtonWidget buttonWidget;
    private final List<ClickableWidget> widgets;

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    @Builder(setterPrefix = "set")
    public ButtonListEntry(Text fieldName, Text buttonText, Consumer<ButtonListEntry> onClickCallback, Supplier<Optional<Text[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        this.buttonWidget = new ButtonWidget(0, 0, 150, 20, buttonText, btn -> onClickCallback.accept(this));
        this.widgets = List.of(buttonWidget);
    }

    public void setButtonText(Text text) {
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

    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = MinecraftClient.getInstance().getWindow();
        buttonWidget.active = isEditable();
        buttonWidget.y = y;
        var displayed = getDisplayedFieldName();
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        if (textRenderer.isRightToLeft()) {
            textRenderer.drawWithShadow(matrices, displayed.asOrderedText(), (float) (window.getScaledWidth() - x - textRenderer.getWidth(displayed)), y + 6, 0xffffff);
            buttonWidget.x = x;
        } else {
            textRenderer.drawWithShadow(matrices, displayed.asOrderedText(), (float) x, (float) (y + 6), this.getPreferredTextColor());
            buttonWidget.x = x + entryWidth - 150;
        }

        buttonWidget.render(matrices, mouseX, mouseY, delta);
    }

    public List<? extends Element> children() {
        return widgets;
    }

    public List<? extends Selectable> narratables() {
        return widgets;
    }
}
