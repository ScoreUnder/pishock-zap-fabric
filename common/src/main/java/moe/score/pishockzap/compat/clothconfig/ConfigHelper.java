package moe.score.pishockzap.compat.clothconfig;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.backend.BackendConnectionTest;
import moe.score.pishockzap.backend.ConnectionTestResult;
import moe.score.pishockzap.backend.model.openshock.ShockCollarModel;
import moe.score.pishockzap.compat.BuilderCompat;
import moe.score.pishockzap.compat.ButtonListEntry;
import moe.score.pishockzap.compat.Translation;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

@Slf4j(topic = Constants.NAME)
public class ConfigHelper {
    private final PishockZapConfig config;
    private final PishockZapConfig defaultConfig;
    private final ConfigBuilder configBuilder;
    private final ConfigEntryBuilder entryBuilder;
    private Consumer<AbstractConfigListEntry<?>> addEntry;
    private final List<Consumer<AbstractConfigListEntry<?>>> addEntryStack = new ArrayList<>();
    private final List<BuilderCompat.SubCategoryBuilderCompat> subCategoryStack = new ArrayList<>();

    public ConfigHelper(PishockZapConfig config, PishockZapConfig defaultConfig, Screen parentScreen) {
        this.config = config;
        this.defaultConfig = defaultConfig;
        this.configBuilder = ConfigBuilder.create()
            .setParentScreen(parentScreen)
            .setTitle(Translation.of("title.pishock-zap.config"));
        this.entryBuilder = configBuilder.entryBuilder();
    }

    public void addBooleanSwitch(String keyPart, Function<PishockZapConfig, Boolean> get, BiConsumer<PishockZapConfig, Boolean> set) {
        add(entryBuilder
            .startBooleanToggle(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .build());
    }

    public @NonNull IntegerSliderEntry addFloatSlider(String keyPart, String formatKeyPart, Function<PishockZapConfig, Float> get, BiConsumer<PishockZapConfig, Float> set, float min, float max) {
        return addFloatSlider(keyPart, formatKeyPart, get, set, min, max, 1000f, 1);
    }

    public @NonNull IntegerSliderEntry addFloatSlider(String keyPart, String formatKeyPart, Function<PishockZapConfig, Float> get, BiConsumer<PishockZapConfig, Float> set, float min, float max, float floatScale, float displayScale) {
        var formatKey = "label.pishock-zap.config." + formatKeyPart;
        var digits = (int) Math.ceil(Math.log10(floatScale / displayScale));
        var formatStr = "%." + digits + "f";

        float value1 = get.apply(config);
        return add(new FloatSliderBuilder(entryBuilder.getResetButtonKey(), Translation.of("title.pishock-zap.config." + keyPart), value1, min, max, floatScale)
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .setTextGetter((value) -> Translation.of(formatKey, String.format(formatStr, value * displayScale)))
            .build());
    }

    public @NotNull IntegerSliderEntry addIntSlider(String keyPart, String formatKeyPart, Function<PishockZapConfig, Integer> get, BiConsumer<PishockZapConfig, Integer> set, int min, int max) {
        return add(entryBuilder
            .startIntSlider(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config), min, max)
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setTextGetter((value) -> Translation.of("label.pishock-zap.config." + formatKeyPart, value))
            .setDefaultValue(get.apply(defaultConfig))
            .build());
    }

    public StringListEntry addTextField(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set) {
        return add(entryBuilder
            .startStrField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .build());
    }

    public StringListEntry addTextFieldNoDefault(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set) {
        return add(entryBuilder
            .startStrField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(config))  // Default value = current value (works as a "discard change" button)
            .build());
    }

    public StringListEntry addTextFieldNoDefault(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set, Function<String, Optional<Component>> errorSupplier) {
        return add(entryBuilder
            .startStrField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(config))  // Default value = current value (works as a "discard change" button)
            .setErrorSupplier(errorSupplier)
            .build());
    }

    public StringListEntry addTextField(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set, Function<String, Optional<Component>> errorSupplier) {
        return add(entryBuilder
            .startStrField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .setErrorSupplier(errorSupplier)
            .build());
    }

    public @NonNull IntegerListEntry makeIntFieldNoDefault(String keyPart, Function<PishockZapConfig, Integer> get, BiConsumer<PishockZapConfig, Integer> set, Function<Integer, Optional<Component>> errorSupplier) {
        return entryBuilder
            .startIntField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(config))
            .setErrorSupplier(errorSupplier)
            .build();
    }

    public @NonNull IntegerListEntry makeIntField(String keyPart, int value) {
        return entryBuilder
            .startIntField(Translation.of("title.pishock-zap.config." + keyPart), value)
            .setDefaultValue(value)
            .build();
    }

    public @NonNull StringListListEntry makeStringListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<String>> get, BiConsumer<PishockZapConfig, List<String>> set, Function<List<String>, Optional<Component>> errorSupplier, Function<String, Optional<Component>> cellErrorSupplier) {
        return entryBuilder
            .startStrList(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setErrorSupplier(errorSupplier)
            .setCellErrorSupplier(cellErrorSupplier)
            .setExpanded(true)
            .setDefaultValue(get.apply(config))  // Default value = current value (works as a "discard change" button)
            .build();
    }

    public @NonNull IntegerListListEntry makeIntListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<Integer>> get, BiConsumer<PishockZapConfig, List<Integer>> set, Function<List<Integer>, Optional<Component>> errorSupplier, Function<Integer, Optional<Component>> cellErrorSupplier) {
        return entryBuilder
            .startIntList(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setErrorSupplier(errorSupplier)
            .setCellErrorSupplier(cellErrorSupplier)
            .setExpanded(true)
            .setDefaultValue(get.apply(config))  // Default value = current value (works as a "discard change" button)
            .build();
    }

    public @NonNull IntegerListListEntry makeIntListField(String keyPart, List<Integer> value) {
        return entryBuilder
            .startIntList(Translation.of("title.pishock-zap.config." + keyPart), value)
            .setExpanded(true)
            .setDefaultValue(value)
            .build();
    }

    public @NonNull AbstractConfigListEntry<ShockDistribution> makeShockDistributionDropdown(String key, Function<PishockZapConfig, ShockDistribution> get, BiConsumer<PishockZapConfig, ShockDistribution> set) {
        return entryBuilder.startEnumSelector(Translation.of("title.pishock-zap.config." + key), ShockDistribution.class, get.apply(config))
            .setDefaultValue(get.apply(defaultConfig))
            .setEnumNameProvider((value) -> Translation.of("enum.pishock-zap.config.shock_distribution." + value.name().toLowerCase()))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + key))
            .build();
    }

    public @NonNull AbstractConfigListEntry<ShockCollarModel> makeOpenShockCollarModelDropdown(String key, ShockCollarModel def) {
        return entryBuilder.startEnumSelector(Translation.of("title.pishock-zap.config." + key), ShockCollarModel.class, def)
            .setDefaultValue(def)
            .setEnumNameProvider((value) -> Translation.raw(value.name()))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + key))
            .build();
    }

    public <T> @NotNull SelectionListEntry<T> addSelector(String keyPart, Function<PishockZapConfig, T> get, BiConsumer<PishockZapConfig, T> set, T[] options, Function<T, Component> nameProvider) {
        var selector = entryBuilder.startSelector(Translation.of("title.pishock-zap.config." + keyPart), options, get.apply(config))
            .setDefaultValue(get.apply(defaultConfig))
            .setNameProvider(nameProvider)
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .build();
        add(selector);
        return selector;
    }

    public <T> @NotNull DropdownBoxEntry<T> makeDropdown(String keyPart, Function<PishockZapConfig, T> get, BiConsumer<PishockZapConfig, T> set, Iterable<T> options, Function<String, T> stringToObject, Function<T, Component> objectToText) {
        return entryBuilder
            .startDropdownMenu(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config), stringToObject, objectToText)
            .setSelections(options)
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .build();
    }

    public <T, W extends AbstractConfigListEntry<T>> NestedListListEntry<T, W> makeNestedList(String keyPart, Function<PishockZapConfig, List<T>> get, BiConsumer<PishockZapConfig, List<T>> set, T defaultNewEntryValue, BiFunction<? super T, ? super NestedListListEntry<T, W>, ? extends W> widgetCreator) {
        return NestedList.<T, W>builder()
            .setTitle(Translation.of("title.pishock-zap.config." + keyPart))
            .setInitialValue(get.apply(config))
            .setTooltipSupplier(ClothUtil.supply(Translation.of("tooltip.pishock-zap.config." + keyPart)))
            .setSaveConsumer(v -> set.accept(config, v))
            .setDefaultValueSupplier(() -> get.apply(defaultConfig))
            .setResetButtonKey(getResetButtonKey())
            .setDefaultNewEntryValue(defaultNewEntryValue)
            .setWidgetCreator(widgetCreator)
            .build();
    }

    public <T> void addSimpleActionButton(String keyPart, Supplier<CompletableFuture<T>> action, Consumer<T> success) {
        addActionButton(keyPart, action, t -> {
            success.accept(t);
            return Translation.of("label.pishock-zap.config." + keyPart);
        });
    }

    public <T> void addActionButton(String keyPart, Supplier<CompletableFuture<T>> action, Function<T, Component> success) {
        add(ButtonListEntry.builder()
            .setButtonText(Translation.of("label.pishock-zap.config." + keyPart))
            .setFieldName(Translation.of("title.pishock-zap.config." + keyPart))
            .setTooltipSupplier(() -> Optional.of(new Component[]{Translation.of("tooltip.pishock-zap.config." + keyPart)}))
            .setOnClickCallback(btn -> {
                btn.setEditable(false);
                btn.setButtonText(Translation.of("label.pishock-zap.config." + keyPart + ".working"));
                action.get()
                    .thenAcceptAsync(t -> {
                        var result = success.apply(t);
                        btn.setEditable(true);
                        btn.setButtonText(result);
                    }, Minecraft.getInstance())
                    .exceptionallyAsync((throwable) -> {
                        if (throwable != null) {
                            log.warn("Error occurred while performing action for button '{}'", keyPart, throwable);
                            btn.setEditable(true);
                            btn.setButtonText(Translation.of("label.pishock-zap.config." + keyPart + ".error"));
                        }
                        return null;
                    }, Minecraft.getInstance());
            })
            .build());
    }

    public void addConnectionTests(Supplier<BackendConnectionTest> connectionTest) {
        addActionButton("api.test_connection",
            () -> connectionTest.get().testConnection(),
            result -> Translation.of("enum.pishock-zap.config.connection_test_result." + result.name().toLowerCase())
                .withStyle(style -> style.withColor(result == ConnectionTestResult.SUCCESS ? ChatFormatting.GREEN : ChatFormatting.RED)));
        addActionButton("api.test_vibration",
            () -> connectionTest.get().testVibration(),
            result -> Translation.of("enum.pishock-zap.config.connection_test_result." + result.name().toLowerCase())
                .withStyle(style -> style.withColor(result == ConnectionTestResult.SUCCESS ? ChatFormatting.GREEN : ChatFormatting.RED)));
    }

    public @NotNull TextListEntry makeTextDescription(Component component) {
        return entryBuilder.startTextDescription(component).build();
    }

    public void addTextDescription(String keyPart) {
        add(makeTextDescription(Translation.of("description.pishock-zap.config." + keyPart)));
    }

    public void addTextDescription(Component component) {
        add(makeTextDescription(component));
    }

    public Component getResetButtonKey() {
        return entryBuilder.getResetButtonKey();
    }

    public <T extends AbstractConfigListEntry<?>> T add(T e) {
        addEntry.accept(e);
        return e;
    }

    public void setCategory(ConfigCategory category) {
        addEntry = category::addEntry;
    }

    public void startCategory(Component title) {
        if (!subCategoryStack.isEmpty()) throw new IllegalStateException();
        var cat = configBuilder.getOrCreateCategory(title);
        setCategory(cat);
    }

    public void startCategory(String keyPart) {
        startCategory(Translation.of("title.pishock-zap.config." + keyPart));
    }

    public BuilderCompat.SubCategoryBuilderCompat startSubCategory(Component title) {
        var cat = BuilderCompat.subCategory(entryBuilder, title);
        if (addEntry != null)
            addEntryStack.add(addEntry);

        addEntry = cat::add;
        subCategoryStack.add(cat);
        cat.setExpanded(true);
        return cat;
    }

    public BuilderCompat.SubCategoryBuilderCompat startSubCategory(String keyPart) {
        return startSubCategory(Translation.of("title.pishock-zap.config." + keyPart));
    }

    public void endSubCategory() {
        addEntry = addEntryStack.remove(addEntryStack.size() - 1);
        addEntry.accept(subCategoryStack.remove(subCategoryStack.size() - 1).build());
    }

    public Screen buildScreen(Runnable saveRunnable) {
        configBuilder.setSavingRunnable(saveRunnable);
        return configBuilder.build();
    }
}
