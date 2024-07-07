package moe.score.pishockzap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import moe.score.pishockzap.compat.FloatSliderBuilder;
import moe.score.pishockzap.compat.Translation;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.PiShockSerialApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static moe.score.pishockzap.pishockapi.PiShockUtils.PISHOCK_MAX_DURATION;
import static moe.score.pishockzap.pishockapi.PiShockUtils.PISHOCK_MAX_INTENSITY;

@SuppressWarnings("unused")
public class PishockZapModConfigMenu implements ModMenuApi {
    private static Screen createConfigScreen(Screen parent) {
        var mod = PishockZapMod.getInstance();
        var config = mod.getConfig();

        var defaultConfig = new PishockZapConfig();

        var configBuilder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Translation.of("title.pishock-zap.config"));

        var generalCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.general"));
        var entryBuilder = configBuilder.entryBuilder();
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(Translation.of("title.pishock-zap.config.general.enabled"), config.isEnabled())
                .setSaveConsumer(config::setEnabled)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.general.enabled"))
                .setDefaultValue(defaultConfig.isEnabled())
                .build());
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(Translation.of("title.pishock-zap.config.general.vibration_only"), config.isVibrationOnly())
                .setSaveConsumer(config::setVibrationOnly)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.general.vibration_only"))
                .setDefaultValue(defaultConfig.isVibrationOnly())
                .build());
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(Translation.of("title.pishock-zap.config.general.shock_on_health"), config.isShockOnHealth())
                .setSaveConsumer(config::setShockOnHealth)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.general.shock_on_health"))
                .setDefaultValue(defaultConfig.isShockOnHealth())
                .build());

        var limitsCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.limits"));

        var shockLimitsCategory = entryBuilder
                .startSubCategory(Translation.of("title.pishock-zap.config.limits.shock_limits"));

        shockLimitsCategory.add(createFloatSlider(entryBuilder, Translation.of("title.pishock-zap.config.limits.duration"), config.getDuration(), 0.1f, PISHOCK_MAX_DURATION)
                .setSaveConsumer(config::setDuration)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.duration"))
                .setDefaultValue(defaultConfig.getDuration())
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.duration").append(String.format("%.3fs", value)))
                .build());
        shockLimitsCategory.add(createFloatSlider(entryBuilder, Translation.of("title.pishock-zap.config.limits.max_duration"), config.getMaxDuration(), 0.1f, PISHOCK_MAX_DURATION)
                .setSaveConsumer(config::setMaxDuration)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.max_duration"))
                .setDefaultValue(defaultConfig.getMaxDuration())
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.duration").append(String.format("%.3fs", value)))
                .build());
        shockLimitsCategory.add(entryBuilder
                .startIntSlider(Translation.of("title.pishock-zap.config.limits.vibration_intensity_min"), config.getVibrationIntensityMin(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setVibrationIntensityMin)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.vibration_intensity_min"))
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.vibration_intensity").append(String.format("%d%%", value)))
                .setDefaultValue(defaultConfig.getVibrationIntensityMin())
                .build());
        shockLimitsCategory.add(entryBuilder
                .startIntSlider(Translation.of("title.pishock-zap.config.limits.vibration_intensity_max"), config.getVibrationIntensityMax(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setVibrationIntensityMax)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.vibration_intensity_max"))
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.vibration_intensity").append(String.format("%d%%", value)))
                .setDefaultValue(defaultConfig.getVibrationIntensityMax())
                .build());
        shockLimitsCategory.add(entryBuilder
                .startIntSlider(Translation.of("title.pishock-zap.config.limits.shock_intensity_min"), config.getShockIntensityMin(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setShockIntensityMin)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.shock_intensity_min"))
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.intensity").append(String.format("%d%%", value)))
                .setDefaultValue(defaultConfig.getShockIntensityMin())
                .build());
        shockLimitsCategory.add(entryBuilder
                .startIntSlider(Translation.of("title.pishock-zap.config.limits.shock_intensity_max"), config.getShockIntensityMax(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setShockIntensityMax)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.shock_intensity_max"))
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.intensity").append(String.format("%d%%", value)))
                .setDefaultValue(defaultConfig.getShockIntensityMax())
                .build());
        shockLimitsCategory.add(createShockDistributionDropdown(entryBuilder, "limits.shock_distribution", config.getShockDistribution(), config::setShockDistribution));

        shockLimitsCategory.setExpanded(true);
        limitsCategory.addEntry(shockLimitsCategory.build());

        var damageThresholdsCategory = entryBuilder
                .startSubCategory(Translation.of("title.pishock-zap.config.limits.damage_thresholds"));

        damageThresholdsCategory.add(createFloatSlider(entryBuilder, Translation.of("title.pishock-zap.config.limits.vibration_threshold"), config.getVibrationThreshold(), 0.0f, 1.0f, 100.0f)
            .setSaveConsumer(config::setVibrationThreshold)
            .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.vibration_threshold"))
            .setTextGetter((value) -> Translation.of("label.pishock-zap.config.hp").append(String.format("%.0f%%", value * 100.0f)))
            .setDefaultValue(defaultConfig.getVibrationThreshold())
            .build());
        damageThresholdsCategory.add(createFloatSlider(entryBuilder, Translation.of("title.pishock-zap.config.limits.min_damage"), config.getMinDamage(), 0.0f, 1.0f, 100.0f)
            .setSaveConsumer(config::setMinDamage)
            .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.min_damage"))
            .setTextGetter((value) -> Translation.of("label.pishock-zap.config.hp").append(String.format("%.0f%%", value * 100.0f)))
            .setDefaultValue(defaultConfig.getMinDamage())
            .build());
        damageThresholdsCategory.add(createFloatSlider(entryBuilder, Translation.of("title.pishock-zap.config.limits.max_damage"), config.getMaxDamage(), 0.0f, 1.0f, 100.0f)
            .setSaveConsumer(config::setMaxDamage)
            .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.max_damage"))
            .setTextGetter((value) -> Translation.of("label.pishock-zap.config.hp").append(String.format("%.0f%%", value * 100.0f)))
            .setDefaultValue(defaultConfig.getMaxDamage())
            .build());

        damageThresholdsCategory.setExpanded(true);
        limitsCategory.addEntry(damageThresholdsCategory.build());

        var shockOnDeathCategory = entryBuilder
                .startSubCategory(Translation.of("title.pishock-zap.config.limits.shock_on_death_category"));

        shockOnDeathCategory.add(entryBuilder
                .startBooleanToggle(Translation.of("title.pishock-zap.config.general.shock_on_death"), config.isShockOnDeath())
                .setSaveConsumer(config::setShockOnDeath)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.general.shock_on_death"))
                .setDefaultValue(defaultConfig.isShockOnDeath())
                .build());
        shockOnDeathCategory.add(entryBuilder
                .startIntSlider(Translation.of("title.pishock-zap.config.limits.shock_intensity_death"), config.getShockIntensityDeath(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setShockIntensityDeath)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.shock_intensity_death"))
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.intensity").append(String.format("%d%%", value)))
                .setDefaultValue(defaultConfig.getShockIntensityDeath())
                .build());
        shockOnDeathCategory.add(createFloatSlider(entryBuilder, Translation.of("title.pishock-zap.config.limits.shock_duration_death"), config.getShockDurationDeath(), 0.1f, PISHOCK_MAX_DURATION)
                .setSaveConsumer(config::setShockDurationDeath)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.limits.shock_duration_death"))
                .setDefaultValue(defaultConfig.getShockDurationDeath())
                .setTextGetter((value) -> Translation.of("label.pishock-zap.config.duration").append(String.format("%.3fs", value)))
                .build());
        shockOnDeathCategory.add(createShockDistributionDropdown(entryBuilder, "limits.shock_distribution_death", config.getShockDistributionDeath(), config::setShockDistributionDeath));

        shockOnDeathCategory.setExpanded(true);
        limitsCategory.addEntry(shockOnDeathCategory.build());

        var debounceCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.debounce"));
        debounceCategory.addEntry(createFloatSlider(entryBuilder, Translation.of("title.pishock-zap.config.debounce.debounce_time"), config.getDebounceTime(), 0.1f, 60.0f)
                .setSaveConsumer(config::setDebounceTime)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.debounce.debounce_time"))
                .setTextGetter((value) -> Text.of(String.format("%.3fs", value)))
                .setDefaultValue(defaultConfig.getDebounceTime())
                .build());
        debounceCategory.addEntry(entryBuilder
                .startBooleanToggle(Translation.of("title.pishock-zap.config.debounce.accumulate_duration"), config.isAccumulateDuration())
                .setSaveConsumer(config::setAccumulateDuration)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.debounce.accumulate_duration"))
                .setDefaultValue(defaultConfig.isAccumulateDuration())
                .build());
        debounceCategory.addEntry(entryBuilder
                .startBooleanToggle(Translation.of("title.pishock-zap.config.debounce.accumulate_intensity"), config.isAccumulateIntensity())
                .setSaveConsumer(config::setAccumulateIntensity)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.debounce.accumulate_intensity"))
                .setDefaultValue(defaultConfig.isAccumulateIntensity())
                .build());
        debounceCategory.addEntry(entryBuilder
                .startBooleanToggle(Translation.of("title.pishock-zap.config.debounce.queue_different"), config.isQueueDifferent())
                .setSaveConsumer(config::setQueueDifferent)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.debounce.queue_different"))
                .setDefaultValue(defaultConfig.isQueueDifferent())
                .build());

        var apiCategory = configBuilder.getOrCreateCategory(Translation.of("title.pishock-zap.config.api"));
        apiCategory.addEntry(entryBuilder
                .startStrField(Translation.of("title.pishock-zap.config.api.log_identifier"), config.getLogIdentifier())
                .setSaveConsumer(config::setLogIdentifier)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.api.log_identifier"))
                .setDefaultValue(defaultConfig.getLogIdentifier())
                .build());
        apiCategory.addEntry(entryBuilder
                .startStrField(Translation.of("title.pishock-zap.config.api.username"), config.getUsername())
                .setSaveConsumer(config::setUsername)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.api.username"))
                // no default
                .build());
        apiCategory.addEntry(entryBuilder
                .startStrField(Translation.of("title.pishock-zap.config.api.api_key"), config.getApiKey())
                .setSaveConsumer(config::setApiKey)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.api.api_key"))
                // no default
                .build());
        apiCategory.addEntry(entryBuilder
                .startStrList(Translation.of("title.pishock-zap.config.api.share_codes"), config.getShareCodes())
                .setSaveConsumer(config::setShareCodes)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.api.share_codes"))
                .setCellErrorSupplier(PishockZapModConfigMenu::isShareCodeInvalid)
                .setExpanded(true)
                // no default
                .build());

        SubCategoryBuilder localApiCategory = entryBuilder
            .startSubCategory(Translation.of("title.pishock-zap.config.api.local"));

        localApiCategory.add(entryBuilder.
                startBooleanToggle(Translation.of("title.pishock-zap.config.api.local.enabled"), config.isLocalEnabled())
                .setSaveConsumer(config::setLocalEnabled)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.api.local.enabled"))
                .setDefaultValue(defaultConfig.isLocalEnabled())
                .build());
        localApiCategory.add(entryBuilder
                .startDropdownMenu(Translation.of("title.pishock-zap.config.api.serial_port"), config.getSerialPort(), Function.identity(), Text::of)
                .setSelections(PiShockSerialApi.getSerialPorts())
                .setSaveConsumer(config::setSerialPort)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.api.serial_port"))
                .setDefaultValue(defaultConfig.getSerialPort())
                .build());
        localApiCategory.add(entryBuilder
                .startIntList(Translation.of("title.pishock-zap.config.api.device_ids"), config.getDeviceIds())
                .setSaveConsumer(config::setDeviceIds)
                .setTooltip(Translation.of("tooltip.pishock-zap.config.api.device_ids"))
                .setExpanded(true)
                // no default
                .build());
        apiCategory.addEntry(localApiCategory.build());

        configBuilder.setSavingRunnable(mod::saveConfig);

        return configBuilder.build();
    }

    private static Optional<Text> isShareCodeInvalid(String shareCode) {
        if (shareCode.isBlank()) return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.entry.empty"));
        if (shareCode.length() < 10 || !shareCode.matches("[0-9A-F]+")) {
            return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.entry.invalid"));
        }
        return Optional.empty();
    }

    private static @NotNull AbstractConfigListEntry<ShockDistribution> createShockDistributionDropdown(ConfigEntryBuilder builder, String key, ShockDistribution def, Consumer<ShockDistribution> saveConsumer) {
        return builder.startEnumSelector(Translation.of("title.pishock-zap.config." + key), ShockDistribution.class, def)
                .setDefaultValue(def)
                .setEnumNameProvider((value) -> Translation.of("enum.pishock-zap.config.shock_distribution." + value.name().toLowerCase()))
                .setSaveConsumer(saveConsumer)
                .setTooltip(Translation.of("tooltip.pishock-zap.config." + key))
                .build();
    }

    private static FloatSliderBuilder createFloatSlider(
        ConfigEntryBuilder entryBuilder,
        Text fieldNameKey,
        float value,
        float min,
        float max
    ) {
        return new FloatSliderBuilder(entryBuilder.getResetButtonKey(), fieldNameKey, value, min, max);
    }

    private static FloatSliderBuilder createFloatSlider(
        ConfigEntryBuilder entryBuilder,
        Text fieldNameKey,
        float value,
        float min,
        float max,
        float floatScale
    ) {
        return new FloatSliderBuilder(entryBuilder.getResetButtonKey(), fieldNameKey, value, min, max, floatScale);
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PishockZapModConfigMenu::createConfigScreen;
    }
}
