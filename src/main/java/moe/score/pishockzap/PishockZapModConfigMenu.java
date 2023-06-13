package moe.score.pishockzap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import moe.score.pishockzap.config.ShockDistribution;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class PishockZapModConfigMenu implements ModMenuApi {
    public static final int PISHOCK_MAX_DURATION = 15;
    public static final int PISHOCK_MAX_INTENSITY = 100;

    private static Screen createConfigScreen(Screen parent) {
        var mod = PishockZapMod.getInstance();
        var config = mod.getConfig();

        var configBuilder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(new TranslatableText("title.pishock-zap.config"));

        var generalCategory = configBuilder.getOrCreateCategory(new TranslatableText("title.pishock-zap.config.general"));
        var entryBuilder = configBuilder.entryBuilder();
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(new TranslatableText("title.pishock-zap.config.general.enabled"), config.isEnabled())
                .setSaveConsumer(config::setEnabled)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.general.enabled"))
                .build());
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(new TranslatableText("title.pishock-zap.config.general.vibration_only"), config.isVibrationOnly())
                .setSaveConsumer(config::setVibrationOnly)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.general.vibration_only"))
                .build());
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(new TranslatableText("title.pishock-zap.config.general.shock_on_death"), config.isShockOnDeath())
                .setSaveConsumer(config::setShockOnDeath)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.general.shock_on_death"))
                .build());
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(new TranslatableText("title.pishock-zap.config.general.shock_on_health"), config.isShockOnHealth())
                .setSaveConsumer(config::setShockOnHealth)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.general.shock_on_health"))
                .build());

        var limitsCategory = configBuilder.getOrCreateCategory(new TranslatableText("title.pishock-zap.config.limits"));
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.duration"), config.getDuration(), 1, PISHOCK_MAX_DURATION)
                .setSaveConsumer(config::setDuration)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.duration"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.max_duration"), config.getMaxDuration(), 1, PISHOCK_MAX_DURATION)
                .setSaveConsumer(config::setMaxDuration)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.max_duration"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.vibration_threshold"), config.getVibrationThreshold(), 0, 20)
                .setSaveConsumer(config::setVibrationThreshold)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.vibration_threshold"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.vibration_intensity_min"), config.getVibrationIntensityMin(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setVibrationIntensityMin)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.vibration_intensity_min"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.vibration_intensity_max"), config.getVibrationIntensityMax(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setVibrationIntensityMax)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.vibration_intensity_max"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.shock_intensity_min"), config.getShockIntensityMin(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setShockIntensityMin)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.shock_intensity_min"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.shock_intensity_max"), config.getShockIntensityMax(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setShockIntensityMax)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.shock_intensity_max"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.shock_intensity_death"), config.getShockIntensityDeath(), 1, PISHOCK_MAX_INTENSITY)
                .setSaveConsumer(config::setShockIntensityDeath)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.shock_intensity_death"))
                .build());
        limitsCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.limits.shock_duration_death"), config.getShockDurationDeath(), 1, PISHOCK_MAX_DURATION)
                .setSaveConsumer(config::setShockDurationDeath)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.limits.shock_duration_death"))
                .build());
        limitsCategory.addEntry(createShockDistributionDropdown(entryBuilder, "limits.shock_distribution", config.getShockDistribution(), config::setShockDistribution));
        limitsCategory.addEntry(createShockDistributionDropdown(entryBuilder, "limits.shock_distribution_death", config.getShockDistributionDeath(), config::setShockDistributionDeath));

        var debounceCategory = configBuilder.getOrCreateCategory(new TranslatableText("title.pishock-zap.config.debounce"));
        debounceCategory.addEntry(entryBuilder
                .startIntSlider(new TranslatableText("title.pishock-zap.config.debounce.debounce_time"), config.getDebounceTime(), 1, 60)
                .setSaveConsumer(config::setDebounceTime)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.debounce.debounce_time"))
                .build());
        debounceCategory.addEntry(entryBuilder
                .startBooleanToggle(new TranslatableText("title.pishock-zap.config.debounce.accumulate_duration"), config.isAccumulateDuration())
                .setSaveConsumer(config::setAccumulateDuration)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.debounce.accumulate_duration"))
                .build());
        debounceCategory.addEntry(entryBuilder
                .startBooleanToggle(new TranslatableText("title.pishock-zap.config.debounce.accumulate_intensity"), config.isAccumulateIntensity())
                .setSaveConsumer(config::setAccumulateIntensity)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.debounce.accumulate_intensity"))
                .build());
        debounceCategory.addEntry(entryBuilder
                .startBooleanToggle(new TranslatableText("title.pishock-zap.config.debounce.queue_different"), config.isQueueDifferent())
                .setSaveConsumer(config::setQueueDifferent)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.debounce.queue_different"))
                .build());

        var apiCategory = configBuilder.getOrCreateCategory(new TranslatableText("title.pishock-zap.config.api"));
        apiCategory.addEntry(entryBuilder
                .startStrField(new TranslatableText("title.pishock-zap.config.api.log_identifier"), config.getLogIdentifier())
                .setSaveConsumer(config::setLogIdentifier)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.api.log_identifier"))
                .build());
        apiCategory.addEntry(entryBuilder
                .startStrField(new TranslatableText("title.pishock-zap.config.api.username"), config.getUsername())
                .setSaveConsumer(config::setUsername)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.api.username"))
                .build());
        apiCategory.addEntry(entryBuilder
                .startStrField(new TranslatableText("title.pishock-zap.config.api.api_key"), config.getApiKey())
                .setSaveConsumer(config::setApiKey)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.api.api_key"))
                .build());
        apiCategory.addEntry(entryBuilder
                .startStrField(new TranslatableText("title.pishock-zap.config.api.share_code"), config.getShareCode())
                .setSaveConsumer(config::setShareCode)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config.api.share_code"))
                .build());

        configBuilder.setSavingRunnable(mod::saveConfig);

        return configBuilder.build();
    }

    private static @NotNull DropdownBoxEntry<ShockDistribution> createShockDistributionDropdown(ConfigEntryBuilder builder, String key, ShockDistribution def, Consumer<ShockDistribution> saveConsumer) {
        return builder.startDropdownMenu(new TranslatableText("title.pishock-zap.config." + key), DropdownMenuBuilder.TopCellElementBuilder.of(def, name -> {
                    try {
                        return ShockDistribution.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }, (sd) -> Text.of(sd.name())))
                .setSelections(Arrays.asList(ShockDistribution.values()))
                .setDefaultValue(def)
                .setSaveConsumer(saveConsumer)
                .setTooltip(new TranslatableText("tooltip.pishock-zap.config." + key))
                .build();
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PishockZapModConfigMenu::createConfigScreen;
    }
}
