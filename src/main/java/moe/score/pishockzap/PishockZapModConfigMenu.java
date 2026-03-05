package moe.score.pishockzap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerListListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackendRegistry;
import moe.score.pishockzap.backend.impls.OpenShockWebApiBackend;
import moe.score.pishockzap.backend.impls.PiShockSerialBackend;
import moe.score.pishockzap.backend.impls.PiShockWebApiV1Backend;
import moe.score.pishockzap.compat.ButtonListEntry;
import moe.score.pishockzap.compat.FloatSliderBuilder;
import moe.score.pishockzap.compat.TextStyle;
import moe.score.pishockzap.compat.Translation;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.mixin.pool.ListEntryExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.Collectors;

import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_DURATION;
import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_INTENSITY;

@SuppressWarnings("unused")
public class PishockZapModConfigMenu implements ModMenuApi {
    public static final String PISHOCK_ACCOUNT_PAGE_URL = "https://pishock.com/#/account";
    private static final String PISHOCK_CONTROLLER_PAGE_URL = "https://pishock.com/#/control";

    private static @NonNull Screen createConfigScreen(Screen parent) {
        var mod = Objects.requireNonNull(PishockZapMod.getInstance(), "PishockZapMod instance is null");
        var config = mod.getConfig();

        var defaultConfig = new PishockZapConfig();

        var configBuilder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Translation.of("title.pishock-zap.config"));
        var entryBuilder = configBuilder.entryBuilder();
        var helper = new Helper(config, defaultConfig, entryBuilder);

        var generalCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.general"));
        helper.setCategory(generalCategory);

        helper.addBooleanSwitch("general.enabled", PishockZapConfig::isEnabled, PishockZapConfig::setEnabled);
        helper.addBooleanSwitch("general.vibration_only", PishockZapConfig::isVibrationOnly, PishockZapConfig::setVibrationOnly);
        helper.addBooleanSwitch("general.shock_on_health", PishockZapConfig::isShockOnHealth, PishockZapConfig::setShockOnHealth);
        helper.addBooleanSwitch("general.fractional_damage", PishockZapConfig::isFractionalDamage, PishockZapConfig::setFractionalDamage);

        var limitsCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.limits"));

        var shockLimitsCategory = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.limits.shock_limits"));
        helper.setCategory(shockLimitsCategory);

        helper.addFloatSlider("limits.duration", "duration", PishockZapConfig::getDuration, PishockZapConfig::setDuration, 0.1f, PISHOCK_MAX_DURATION);
        helper.addFloatSlider("limits.max_duration", "duration", PishockZapConfig::getMaxDuration, PishockZapConfig::setMaxDuration, 0.1f, PISHOCK_MAX_DURATION);
        helper.addIntSlider("limits.vibration_intensity_min", "vibration_intensity", PishockZapConfig::getVibrationIntensityMin, PishockZapConfig::setVibrationIntensityMin, 1, PISHOCK_MAX_INTENSITY);
        helper.addIntSlider("limits.vibration_intensity_max", "vibration_intensity", PishockZapConfig::getVibrationIntensityMax, PishockZapConfig::setVibrationIntensityMax, 1, PISHOCK_MAX_INTENSITY);
        helper.addIntSlider("limits.shock_intensity_min", "intensity", PishockZapConfig::getShockIntensityMin, PishockZapConfig::setShockIntensityMin, 1, PISHOCK_MAX_INTENSITY);
        helper.addIntSlider("limits.shock_intensity_max", "intensity", PishockZapConfig::getShockIntensityMax, PishockZapConfig::setShockIntensityMax, 1, PISHOCK_MAX_INTENSITY);
        shockLimitsCategory.add(createShockDistributionDropdown(entryBuilder, "limits.shock_distribution", config.getShockDistribution(), config::setShockDistribution));

        shockLimitsCategory.setExpanded(true);
        limitsCategory.addEntry(shockLimitsCategory.build());

        var damageThresholdsCategory = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.limits.damage_thresholds"));
        helper.setCategory(damageThresholdsCategory);

        helper.addFloatSlider("limits.vibration_threshold", "hp", PishockZapConfig::getVibrationThreshold, PishockZapConfig::setVibrationThreshold, 0, 1, 100, 100);
        helper.addFloatSlider("limits.min_damage", "hp", PishockZapConfig::getMinDamage, PishockZapConfig::setMinDamage, 0, 1, 100, 100);
        helper.addFloatSlider("limits.max_damage", "hp", PishockZapConfig::getMaxDamage, PishockZapConfig::setMaxDamage, 0, 1, 100, 100);

        damageThresholdsCategory.setExpanded(true);
        limitsCategory.addEntry(damageThresholdsCategory.build());

        var shockOnDeathCategory = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.limits.shock_on_death_category"));
        helper.setCategory(shockOnDeathCategory);

        helper.addBooleanSwitch("general.shock_on_death", PishockZapConfig::isShockOnDeath, PishockZapConfig::setShockOnDeath);
        helper.addIntSlider("limits.shock_intensity_death", "intensity", PishockZapConfig::getShockIntensityDeath, PishockZapConfig::setShockIntensityDeath, 1, PISHOCK_MAX_INTENSITY);
        helper.addFloatSlider("limits.shock_duration_death", "duration", PishockZapConfig::getShockDurationDeath, PishockZapConfig::setShockDurationDeath, 0.1f, PISHOCK_MAX_DURATION);
        shockOnDeathCategory.add(createShockDistributionDropdown(entryBuilder, "limits.shock_distribution_death", config.getShockDistributionDeath(), config::setShockDistributionDeath));

        shockOnDeathCategory.setExpanded(true);
        limitsCategory.addEntry(shockOnDeathCategory.build());

        var debounceCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.debounce"));
        helper.setCategory(debounceCategory);

        helper.addFloatSlider("debounce.debounce_time", "time_interval", PishockZapConfig::getDebounceTime, PishockZapConfig::setDebounceTime, 0.1f, 60f);
        helper.addBooleanSwitch("debounce.accumulate_duration", PishockZapConfig::isAccumulateDuration, PishockZapConfig::setAccumulateDuration);
        helper.addBooleanSwitch("debounce.accumulate_intensity", PishockZapConfig::isAccumulateIntensity, PishockZapConfig::setAccumulateIntensity);
        helper.addBooleanSwitch("debounce.queue_different", PishockZapConfig::isQueueDifferent, PishockZapConfig::setQueueDifferent);

        var apiCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.api"));

        var apiTypeSwitcher = entryBuilder.startSelector(Translation.of("title.pishock-zap.config.api_type"), ShockBackendRegistry.getAllBackendIds(), config.getApiType())
            .setDefaultValue(config.getApiType())
            .setNameProvider(value -> Translation.of(ShockBackendRegistry.getTranslationKey(value)))
            .setSaveConsumer(config::setApiType)
            .setTooltip(Translation.of("tooltip.pishock-zap.config.api_type"))
            .build();

        apiCategory.addEntry(apiTypeSwitcher);

        apiCategory.addEntry(entryBuilder.startTextDescription(
            Translation.of("description.pishock-zap.config.api_type",
                Translation.of("enum.pishock-zap.config.api_type.web_v1").styled(style -> style.withBold(true))
            )).build());

        var webV1Category = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.api.web_v1"))
            .setExpanded(true)
            .setDisplayRequirement(() -> DefaultShockBackends.PISHOCK_WEB_V1.equals(apiTypeSwitcher.getValue()));
        helper.setCategory(webV1Category);

        var logIdentifierField = helper.addTextField("api.log_identifier", PishockZapConfig::getLogIdentifier, PishockZapConfig::setLogIdentifier);

        webV1Category.add(entryBuilder.startTextDescription(
                Translation.of("description.pishock-zap.config.api.web_v1",
                    Translation.addLink(
                        Translation.of("description.pishock-zap.config.api.web_v1.api_key_link"),
                        PISHOCK_ACCOUNT_PAGE_URL
                    ),
                    Translation.addLink(
                        Translation.of("description.pishock-zap.config.api.web_v1.share_codes_link"),
                        PISHOCK_CONTROLLER_PAGE_URL
                    )
                ))
            .build());

        var piShockUsernameEntry = helper.addTextFieldNoDefault("api.username", PishockZapConfig::getUsername, PishockZapConfig::setUsername);
        var piShockApiKeyEntry = helper.addTextFieldNoDefault("api.api_key", PishockZapConfig::getApiKey, PishockZapConfig::setApiKey);
        StringListListEntry piShockShareCodesField = helper.makeStringListFieldNoDefault("api.share_codes", PishockZapConfig::getShareCodes, PishockZapConfig::setShareCodes, list -> {
            if (DefaultShockBackends.PISHOCK_WEB_V1.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (list.isEmpty()) return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.empty"));
            return Optional.empty();
        }, shareCode -> {
            if (DefaultShockBackends.PISHOCK_WEB_V1.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            return isShareCodeInvalid(shareCode);
        });

        if (piShockShareCodesField instanceof ListEntryExt piShockSerialDeviceIdsExt) {
            helper.addActionButton(
                "api.web_v1.add_my_ids",
                () -> new PiShockWebApiV1Backend.HttpBackend().probeShareCodes(piShockUsernameEntry.getValue(), piShockApiKeyEntry.getValue()),
                piShockSerialDeviceIdsExt::pishockZap$addListEntries);
        }

        helper.add(piShockShareCodesField);

        webV1Category.add(entryBuilder.startTextDescription(
            Translation.of("description.pishock-zap.config.api.web_v1.disclaimer")).build());

        apiCategory.addEntry(webV1Category.build());

        SubCategoryBuilder localApiCategory = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.api.local"))
            .setExpanded(true)
            .setDisplayRequirement(() -> DefaultShockBackends.PISHOCK_SERIAL.equals(apiTypeSwitcher.getValue()));
        helper.setCategory(localApiCategory);

        localApiCategory.add(entryBuilder.startTextDescription(Translation.of("description.pishock-zap.config.api.local"))
            .build());

        var piShockSerialPortEntry = entryBuilder
            .startDropdownMenu(Translation.of("title.pishock-zap.config.api.serial_port"), config.getSerialPort(), Function.identity(), Text::of)
            .setSelections(PiShockSerialBackend.getSerialPorts())
            .setSaveConsumer(config::setSerialPort)
            .setTooltip(Translation.of("tooltip.pishock-zap.config.api.serial_port"))
            .setDefaultValue(defaultConfig.getSerialPort())
            .build();
        localApiCategory.add(piShockSerialPortEntry);

        localApiCategory.add(entryBuilder.startTextDescription(
            Translation.of("description.pishock-zap.config.api.local.device_ids",
                Translation.addLink(
                    Translation.of("description.pishock-zap.config.api.local.device_ids.link"),
                    PISHOCK_CONTROLLER_PAGE_URL
                ))
        ).build());

        var piShockSerialDeviceIdField = helper.makeIntListFieldNoDefault("api.device_ids", PishockZapConfig::getDeviceIds, PishockZapConfig::setDeviceIds, list -> {
            if (DefaultShockBackends.PISHOCK_SERIAL.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (list.isEmpty())
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.list_empty"));
            return Optional.empty();
        }, id -> {
            if (DefaultShockBackends.PISHOCK_SERIAL.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (id == null || id < 0)
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.must_be_positive"));
            if (id >= 65536 * 4)
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.too_high"));
            return Optional.empty();
        });

        if (piShockSerialDeviceIdField instanceof ListEntryExt piShockSerialDeviceIdsExt) {
            helper.addActionButton(
                "api.local.add_my_ids",
                () -> PiShockSerialBackend.probeDeviceIds(piShockSerialPortEntry.getValue()),
                piShockSerialDeviceIdsExt::pishockZap$addListEntries);
        }

        helper.add(piShockSerialDeviceIdField);

        apiCategory.addEntry(localApiCategory.build());

        var webhookCategory = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.api.webhook"))
            .setExpanded(true)
            .setDisplayRequirement(() -> DefaultShockBackends.WEBHOOK.equals(apiTypeSwitcher.getValue()));
        helper.setCategory(webhookCategory);

        helper.addTextField("api.custom_webhook_url", PishockZapConfig::getCustomWebhookUrl, PishockZapConfig::setCustomWebhookUrl, url -> {
            if (DefaultShockBackends.WEBHOOK.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (url.isBlank())
                return Optional.of(Translation.of("error.pishock-zap.config.api.custom_webhook_url.empty"));
            try {
                new URL(url);
            } catch (Exception e) {
                return Optional.of(Translation.of("error.pishock-zap.config.api.custom_webhook_url.invalid"));
            }
            return Optional.empty();
        });

        var allOpTypes = Arrays.stream(OpType.values())
            .map(OpType::name)
            .collect(Collectors.joining("\", \"", "\"", "\""));

        var allShockDistributions = Arrays.stream(ShockDistribution.values())
            .map(ShockDistribution::name)
            .collect(Collectors.joining("\", \"", "\"", "\""));

        webhookCategory.add(entryBuilder.startTextDescription(
            Translation.of("description.pishock-zap.config.api.webhook",
                Translation.of("description.pishock-zap.config.api.webhook.payload",
                    Translation.raw("\"" + OpType.SHOCK.name() + "\"").styled(style ->
                        TextStyle.setHoverText(style.withColor(Formatting.LIGHT_PURPLE).withUnderline(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.operation", allOpTypes))),
                    Translation.raw("26").styled(style ->
                        TextStyle.setHoverText(style.withColor(Formatting.LIGHT_PURPLE).withUnderline(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.intensity"))),
                    Translation.raw("1.2").styled(style ->
                        TextStyle.setHoverText(style.withColor(Formatting.LIGHT_PURPLE).withUnderline(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.duration"))),
                    Translation.raw("\"" + ShockDistribution.RANDOM.name() + "\"").styled(style ->
                        TextStyle.setHoverText(style.withColor(Formatting.LIGHT_PURPLE).withUnderline(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.distribution", allShockDistributions)))
                ).styled(style -> style.withColor(Formatting.GRAY))
            )).build());

        apiCategory.addEntry(webhookCategory.build());

        var openShockApiCategory = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.api.openshock"))
            .setExpanded(true)
            .setDisplayRequirement(() -> DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue()));
        helper.setCategory(openShockApiCategory);

        var openShockApiTokenField = helper.addTextField(
            "api.openshock.api_token", PishockZapConfig::getOpenShockApiToken, PishockZapConfig::setOpenShockApiToken, tok -> {
                if (DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue())) return Optional.empty();
                if (tok.isBlank())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.api_token.empty"));
                return Optional.empty();
            });

        var openShockDeviceIdField = helper.makeStringListFieldNoDefault(
            "api.openshock.device_ids", PishockZapConfig::getOpenShockShockerIds, PishockZapConfig::setOpenShockShockerIds,
            l -> {
                if (DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue())) return Optional.empty();
                if (l.isEmpty())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.device_ids.list_empty"));
                return Optional.empty();
            },
            id -> {
                if (DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue())) return Optional.empty();
                if (id.isBlank())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.device_ids.empty"));
                return Optional.empty();
            }
        );

        if (openShockDeviceIdField instanceof ListEntryExt openShockDeviceIdsExt) {
            helper.addActionButton(
                "api.openshock.add_my_ids",
                () -> OpenShockWebApiBackend.probeDeviceIds(openShockApiTokenField.getValue()),
                openShockDeviceIdsExt::pishockZap$addListEntries);
        }

        helper.add(openShockDeviceIdField);
        helper.add(logIdentifierField);

        apiCategory.addEntry(openShockApiCategory.build());

        configBuilder.setSavingRunnable(mod::saveConfig);

        return configBuilder.build();
    }

    private static @NonNull Optional<Text> isShareCodeInvalid(@NonNull String shareCode) {
        if (shareCode.isBlank())
            return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.entry.empty"));
        if (shareCode.length() < 10 || !shareCode.matches("[0-9A-F]+")) {
            return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.entry.invalid"));
        }
        return Optional.empty();
    }

    private static @NonNull AbstractConfigListEntry<ShockDistribution> createShockDistributionDropdown(@NonNull ConfigEntryBuilder builder, String key, ShockDistribution def, Consumer<ShockDistribution> saveConsumer) {
        return builder.startEnumSelector(Translation.of("title.pishock-zap.config." + key), ShockDistribution.class, def)
            .setDefaultValue(def)
            .setEnumNameProvider((value) -> Translation.of("enum.pishock-zap.config.shock_distribution." + value.name().toLowerCase()))
            .setSaveConsumer(saveConsumer)
            .setTooltip(Translation.of("tooltip.pishock-zap.config." + key))
            .build();
    }

    @Override
    public @NonNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PishockZapModConfigMenu::createConfigScreen;
    }

    @RequiredArgsConstructor
    private static class Helper {
        private final PishockZapConfig config;
        private final PishockZapConfig defaultConfig;
        private final ConfigEntryBuilder entryBuilder;
        private Consumer<AbstractConfigListEntry<?>> addEntry;

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

        public StringListEntry addTextField(String keyPart, Function<PishockZapConfig, String> get, BiConsumer<PishockZapConfig, String> set, Function<String, Optional<Text>> errorSupplier) {
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

        public void addStringListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<String>> get, BiConsumer<PishockZapConfig, List<String>> set, Function<List<String>, Optional<Text>> errorSupplier, Function<String, Optional<Text>> cellErrorSupplier) {
            add(makeStringListFieldNoDefault(keyPart, get, set, errorSupplier, cellErrorSupplier));
        }

        public @NonNull StringListListEntry makeStringListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<String>> get, BiConsumer<PishockZapConfig, List<String>> set, Function<List<String>, Optional<Text>> errorSupplier, Function<String, Optional<Text>> cellErrorSupplier) {
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

        public void addIntListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<Integer>> get, BiConsumer<PishockZapConfig, List<Integer>> set, Function<List<Integer>, Optional<Text>> errorSupplier, Function<Integer, Optional<Text>> cellErrorSupplier) {
            add(makeIntListFieldNoDefault(keyPart, get, set, errorSupplier, cellErrorSupplier));
        }

        public @NonNull IntegerListListEntry makeIntListFieldNoDefault(String keyPart, Function<PishockZapConfig, List<Integer>> get, BiConsumer<PishockZapConfig, List<Integer>> set, Function<List<Integer>, Optional<Text>> errorSupplier, Function<Integer, Optional<Text>> cellErrorSupplier) {
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

        public <T> void addActionButton(String keyPart, Supplier<CompletableFuture<T>> action, Consumer<T> success) {
            add(ButtonListEntry.builder()
                .setButtonText(Translation.of("label.pishock-zap.config." + keyPart))
                .setFieldName(Translation.of("title.pishock-zap.config." + keyPart))
                .setTooltipSupplier(() -> Optional.of(new Text[]{Translation.of("tooltip.pishock-zap.config." + keyPart)}))
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

                        private void onError(Throwable throwable) {
                            throwable.printStackTrace();
                            btn.setEditable(true);
                            btn.setButtonText(Translation.of("label.pishock-zap.config." + keyPart + ".error"));
                        }
                    }, MinecraftClient.getInstance());
                })
                .build());
        }

        public void add(AbstractConfigListEntry<?> e) {
            addEntry.accept(e);
        }

        public void setCategory(ConfigCategory category) {
            addEntry = category::addEntry;
        }

        public void setCategory(SubCategoryBuilder category) {
            addEntry = category::add;
        }
    }
}
