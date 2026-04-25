package moe.score.pishockzap.compat.clothconfig;

import lombok.NonNull;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import moe.score.pishockzap.backend.model.openshock.ShockCollarModel;
import moe.score.pishockzap.compat.BuilderCompat;
import moe.score.pishockzap.compat.ButtonListEntry;
import moe.score.pishockzap.compat.Translation;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

public class ConfigHelper {
    private final PishockZapConfig config;
    private final PishockZapConfig defaultConfig;
    private final ConfigBuilder configBuilder;
    private final ConfigEntryBuilder entryBuilder;
    private Consumer<AbstractConfigListEntry<?>> addEntry;
    private final List<Consumer<AbstractConfigListEntry<?>>> addEntryStack = new ArrayList<>();
    private final List<BuilderCompat.SubCategoryBuilderCompat> subCategoryStack = new ArrayList<>();

    public ConfigHelper(PishockZapConfig config, PishockZapConfig defaultConfig, ConfigBuilder configBuilder) {
        this.config = config;
        this.defaultConfig = defaultConfig;
        this.configBuilder = configBuilder;
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

    public void addFloatSlider(String keyPart, String formatKeyPart, Function<PishockZapConfig, Float> get, BiConsumer<PishockZapConfig, Float> set, float min, float max) {
        addFloatSlider(keyPart, formatKeyPart, get, set, min, max, 1000f, 1);
    }

    public void addFloatSlider(String keyPart, String formatKeyPart, Function<PishockZapConfig, Float> get, BiConsumer<PishockZapConfig, Float> set, float min, float max, float floatScale, float displayScale) {
        var formatKey = "label.pishock-zap.config." + formatKeyPart;
        var digits = (int) Math.ceil(Math.log10(floatScale / displayScale));
        var formatStr = "%." + digits + "f";

        float value1 = get.apply(config);
        add(new FloatSliderBuilder(entryBuilder.getResetButtonKey(), Translation.of("title.pishock-zap.config." + keyPart), value1, min, max, floatScale)
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .setTextGetter((value) -> Translation.of(formatKey, String.format(formatStr, value * displayScale)))
            .build());
    }

    public void addIntSlider(String keyPart, String formatKeyPart, Function<PishockZapConfig, Integer> get, BiConsumer<PishockZapConfig, Integer> set, int min, int max) {
        add(entryBuilder
            .startIntSlider(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config), min, max)
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setTextGetter((value) -> Translation.of("label.pishock-zap.config." + formatKeyPart, value))
            .setDefaultValue(get.apply(defaultConfig))
            .build());
    }

    public StringListEntry addTextField(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set) {
        StringListEntry field = entryBuilder
            .startStrField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .build();
        add(field);
        return field;
    }

    public StringListEntry addTextFieldNoDefault(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set) {
        StringListEntry entry = entryBuilder
            .startStrField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            // no default
            .build();
        add(entry);
        return entry;
    }

    public StringListEntry addTextField(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set, Function<String, Optional<Component>> errorSupplier) {
        StringListEntry field = entryBuilder
            .startStrField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .setErrorSupplier(errorSupplier)
            .build();
        add(field);
        return field;
    }

    public IntegerListEntry addIntField(String keyPart, Function<PishockZapConfig, Integer> get, BiConsumer<PishockZapConfig, Integer> set, Function<Integer, Optional<Component>> errorSupplier) {
        var field = makeIntField(keyPart, get, set, errorSupplier);
        add(field);
        return field;
    }

    public @NonNull IntegerListEntry makeIntField(String keyPart, Function<PishockZapConfig, Integer> get, BiConsumer<PishockZapConfig, Integer> set, Function<Integer, Optional<Component>> errorSupplier) {
        return entryBuilder
            .startIntField(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setDefaultValue(get.apply(defaultConfig))
            .setErrorSupplier(errorSupplier)
            .build();
    }

    public @NonNull IntegerListEntry makeIntField(String keyPart, int value) {
        return entryBuilder
            .startIntField(Translation.of("title.pishock-zap.config." + keyPart), value)
            .build();
    }

    public void addStringListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<String>> get, BiConsumer<PishockZapConfig, List<String>> set, Function<List<String>, Optional<Component>> errorSupplier, Function<String, Optional<Component>> cellErrorSupplier) {
        add(makeStringListFieldNoDefault(keyPart, get, set, errorSupplier, cellErrorSupplier));
    }

    public @NonNull StringListListEntry makeStringListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<String>> get, BiConsumer<PishockZapConfig, List<String>> set, Function<List<String>, Optional<Component>> errorSupplier, Function<String, Optional<Component>> cellErrorSupplier) {
        return entryBuilder
            .startStrList(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setErrorSupplier(errorSupplier)
            .setCellErrorSupplier(cellErrorSupplier)
            .setExpanded(true)
            // no default
            .build();
    }

    public void addIntListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<Integer>> get, BiConsumer<PishockZapConfig, List<Integer>> set, Function<List<Integer>, Optional<Component>> errorSupplier, Function<Integer, Optional<Component>> cellErrorSupplier) {
        add(makeIntListFieldNoDefault(keyPart, get, set, errorSupplier, cellErrorSupplier));
    }

    public @NonNull IntegerListListEntry makeIntListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<Integer>> get, BiConsumer<PishockZapConfig, List<Integer>> set, Function<List<Integer>, Optional<Component>> errorSupplier, Function<Integer, Optional<Component>> cellErrorSupplier) {
        return entryBuilder
            .startIntList(Translation.of("title.pishock-zap.config." + keyPart), get.apply(config))
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .setErrorSupplier(errorSupplier)
            .setCellErrorSupplier(cellErrorSupplier)
            .setExpanded(true)
            // no default
            .build();
    }

    public @NonNull IntegerListListEntry makeIntListField(String keyPart, List<Integer> value) {
        return entryBuilder
            .startIntList(Translation.of("title.pishock-zap.config." + keyPart), value)
            .setExpanded(true)
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

    public <T> @NotNull SelectionListEntry<T> makeSelector(String keyPart, Function<PishockZapConfig, T> get, BiConsumer<PishockZapConfig, T> set, T[] options, Function<T, Component> nameProvider) {
        return entryBuilder.startSelector(Translation.of("title.pishock-zap.config." + keyPart), options, get.apply(config))
            .setDefaultValue(get.apply(defaultConfig))
            .setNameProvider(nameProvider)
            .setSaveConsumer(v -> set.accept(config, v))
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + keyPart))
            .build();
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

    public <T> void addActionButton(String keyPart, Supplier<CompletableFuture<T>> action, Consumer<T> success) {
        add(ButtonListEntry.builder()
            .setButtonText(Translation.of("label.pishock-zap.config." + keyPart))
            .setFieldName(Translation.of("title.pishock-zap.config." + keyPart))
            .setTooltipSupplier(() -> Optional.of(new Component[]{Translation.of("tooltip.pishock-zap.config." + keyPart)}))
            .setOnClickCallback(btn -> {
                btn.setEditable(false);
                btn.setButtonText(Translation.of("label.pishock-zap.config." + keyPart + ".working"));
                action.get().handleAsync(new BiFunction<>() {
                    @Override
                    public Object apply(T t, Throwable throwable) {
                        if (throwable != null) {
                            onError(throwable);
                        } else {
                            try {
                                success.accept(t);
                                btn.setEditable(true);
                                btn.setButtonText(Translation.of("label.pishock-zap.config." + keyPart));
                            } catch (Exception e) {
                                onError(e);
                            }
                        }
                        return null;
                    }

                    @SuppressWarnings("CallToPrintStackTrace")
                    private void onError(Throwable throwable) {
                        throwable.printStackTrace();
                        btn.setEditable(true);
                        btn.setButtonText(Translation.of("label.pishock-zap.config." + keyPart + ".error"));
                    }
                }, Minecraft.getInstance());
            })
            .build());
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

    public void add(AbstractConfigListEntry<?> e) {
        addEntry.accept(e);
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
}
