package moe.score.pishockzap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.NonNull;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackendRegistry;
import moe.score.pishockzap.backend.impls.OpenShockWebApiBackend;
import moe.score.pishockzap.backend.impls.PiShockSerialBackend;
import moe.score.pishockzap.backend.impls.PiShockWebApiV1Backend;
import moe.score.pishockzap.backend.model.openshock.ShockCollarModel;
import moe.score.pishockzap.backend.model.openshock.ShockDevice;
import moe.score.pishockzap.compat.TextStyle;
import moe.score.pishockzap.compat.Translation;
import moe.score.pishockzap.compat.clothconfig.Arity2StructEntry;
import moe.score.pishockzap.compat.clothconfig.ClothUtil;
import moe.score.pishockzap.compat.clothconfig.ConfigHelper;
import moe.score.pishockzap.compat.clothconfig.NestedList;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.mixin.pool.ListEntryExt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_DURATION;
import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_INTENSITY;

@SuppressWarnings("unused")
@ApiStatus.Internal
public class PishockZapModConfigMenu implements ModMenuApi {
    public static final String PISHOCK_ACCOUNT_PAGE_URL = "https://login.pishock.com/account";
    private static final String PISHOCK_CONTROLLER_PAGE_URL = "https://pishock.com/#/control";

    private static @NonNull Screen createConfigScreen(Screen parent) {
        var mod = Objects.requireNonNull(PishockZapMod.getInstance(), "PishockZapMod instance is null");
        var config = mod.getConfig();

        var defaultConfig = new PishockZapConfig();

        var configBuilder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Translation.of("title.pishock-zap.config"));
        var helper = new ConfigHelper(config, defaultConfig, configBuilder);

        addGeneralCategory(helper);
        addLimitsCategory(helper);
        addDebounceCategory(helper);
        addApiCategory(helper, config, defaultConfig);

        configBuilder.setSavingRunnable(mod::saveConfig);
        return configBuilder.build();
    }

    private static void addGeneralCategory(ConfigHelper helper) {
        helper.startCategory("general");

        helper.addBooleanSwitch("general.enabled", PishockZapConfig::isEnabled, PishockZapConfig::setEnabled);
        helper.addBooleanSwitch("general.vibration_only", PishockZapConfig::isVibrationOnly, PishockZapConfig::setVibrationOnly);
        helper.addBooleanSwitch("general.shock_on_health", PishockZapConfig::isShockOnHealth, PishockZapConfig::setShockOnHealth);
        helper.addBooleanSwitch("general.fractional_damage", PishockZapConfig::isFractionalDamage, PishockZapConfig::setFractionalDamage);
    }

    private static void addLimitsCategory(ConfigHelper helper) {
        helper.startCategory("limits");

        addShockLimitsSubCategory(helper);
        addDamageThresholdsSubCategory(helper);
        addShockOnDeathSubCategory(helper);
    }

    private static void addShockLimitsSubCategory(ConfigHelper helper) {
        helper.startSubCategory(Translation.of("title.pishock-zap.config.limits.shock_limits"));

        helper.addFloatSlider("limits.duration", "duration", PishockZapConfig::getDuration, PishockZapConfig::setDuration, 0.1f, PISHOCK_MAX_DURATION);
        helper.addFloatSlider("limits.max_duration", "duration", PishockZapConfig::getMaxDuration, PishockZapConfig::setMaxDuration, 0.1f, PISHOCK_MAX_DURATION);
        helper.addIntSlider("limits.vibration_intensity_min", "vibration_intensity", PishockZapConfig::getVibrationIntensityMin, PishockZapConfig::setVibrationIntensityMin, 1, PISHOCK_MAX_INTENSITY);
        helper.addIntSlider("limits.vibration_intensity_max", "vibration_intensity", PishockZapConfig::getVibrationIntensityMax, PishockZapConfig::setVibrationIntensityMax, 1, PISHOCK_MAX_INTENSITY);
        helper.addIntSlider("limits.shock_intensity_min", "intensity", PishockZapConfig::getShockIntensityMin, PishockZapConfig::setShockIntensityMin, 1, PISHOCK_MAX_INTENSITY);
        helper.addIntSlider("limits.shock_intensity_max", "intensity", PishockZapConfig::getShockIntensityMax, PishockZapConfig::setShockIntensityMax, 1, PISHOCK_MAX_INTENSITY);
        helper.add(helper.makeShockDistributionDropdown("limits.shock_distribution", PishockZapConfig::getShockDistribution, PishockZapConfig::setShockDistribution));

        helper.endSubCategory();
    }

    private static void addDamageThresholdsSubCategory(ConfigHelper helper) {
        helper.startSubCategory(Translation.of("title.pishock-zap.config.limits.damage_thresholds"));

        helper.addFloatSlider("limits.vibration_threshold", "hp", PishockZapConfig::getVibrationThreshold, PishockZapConfig::setVibrationThreshold, 0, 1, 100, 100);
        helper.addFloatSlider("limits.min_damage", "hp", PishockZapConfig::getMinDamage, PishockZapConfig::setMinDamage, 0, 1, 100, 100);
        helper.addFloatSlider("limits.max_damage", "hp", PishockZapConfig::getMaxDamage, PishockZapConfig::setMaxDamage, 0, 1, 100, 100);

        helper.endSubCategory();
    }

    private static void addShockOnDeathSubCategory(ConfigHelper helper) {
        helper.startSubCategory(Translation.of("title.pishock-zap.config.limits.shock_on_death_category"));

        helper.addBooleanSwitch("general.shock_on_death", PishockZapConfig::isShockOnDeath, PishockZapConfig::setShockOnDeath);
        helper.addIntSlider("limits.shock_intensity_death", "intensity", PishockZapConfig::getShockIntensityDeath, PishockZapConfig::setShockIntensityDeath, 1, PISHOCK_MAX_INTENSITY);
        helper.addFloatSlider("limits.shock_duration_death", "duration", PishockZapConfig::getShockDurationDeath, PishockZapConfig::setShockDurationDeath, 0.1f, PISHOCK_MAX_DURATION);
        helper.add(helper.makeShockDistributionDropdown("limits.shock_distribution_death", PishockZapConfig::getShockDistributionDeath, PishockZapConfig::setShockDistributionDeath));

        helper.endSubCategory();
    }

    private static void addDebounceCategory(ConfigHelper helper) {
        helper.startCategory("debounce");

        helper.addFloatSlider("debounce.debounce_time", "time_interval", PishockZapConfig::getDebounceTime, PishockZapConfig::setDebounceTime, 0.1f, 60f);
        helper.addBooleanSwitch("debounce.accumulate_duration", PishockZapConfig::isAccumulateDuration, PishockZapConfig::setAccumulateDuration);
        helper.addBooleanSwitch("debounce.accumulate_intensity", PishockZapConfig::isAccumulateIntensity, PishockZapConfig::setAccumulateIntensity);
        helper.addBooleanSwitch("debounce.queue_different", PishockZapConfig::isQueueDifferent, PishockZapConfig::setQueueDifferent);
    }

    private static int specialOrderingKey(String backendType) {
        return switch (backendType) {
            case DefaultShockBackends.PISHOCK_WEBSOCKET -> 0;
            case DefaultShockBackends.PISHOCK_SERIAL -> 1;
            case DefaultShockBackends.OPENSHOCK_WEB -> 2;
            case DefaultShockBackends.OPENSHOCK_SERIAL -> 3;
            case DefaultShockBackends.PISHOCK_WEB_V1 -> 4;
            default -> Integer.MAX_VALUE;
        };
    }

    private static Component getApiTypeTranslation(String apiType) {
        var text = Translation.of(ShockBackendRegistry.getTranslationKey(apiType));
        return switch (apiType) {
            case DefaultShockBackends.PISHOCK_WEB_V1 -> text.withStyle(style -> style.withStrikethrough(true));
            default -> text;
        };
    }

    private static void addApiCategory(ConfigHelper helper, PishockZapConfig config, PishockZapConfig defaultConfig) {
        helper.startCategory("api");

        var apiTypeSwitcher = helper.makeSelector(
            "api_type",
            PishockZapConfig::getApiType,
            PishockZapConfig::setApiType,
            Arrays.stream(ShockBackendRegistry.getAllBackendIds())
                .sorted(Comparator.comparingInt(PishockZapModConfigMenu::specialOrderingKey).thenComparing(a -> a))
                .toArray(String[]::new),
            PishockZapModConfigMenu::getApiTypeTranslation);

        helper.add(apiTypeSwitcher);

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api_type",
                Translation.of("enum.pishock-zap.config.api_type.websocket").withStyle(style -> style.withBold(true))));

        var psFields = addPishockWebV1ApiSubCategory(helper, apiTypeSwitcher);
        var piShockSerialPortEntry = addPishockLocalSerialApiSubCategory(helper, apiTypeSwitcher);
        addWebHookApiSubCategory(helper, apiTypeSwitcher);
        var openShockApiTokenField = addOpenShockWebApiSubCategory(helper, apiTypeSwitcher, psFields.logIdentifierField());
        addOpenShockSerialApiSubCategory(helper, config, defaultConfig, apiTypeSwitcher, openShockApiTokenField, piShockSerialPortEntry);
        addPishockWebSocketApiSubCategory(helper, config, defaultConfig, apiTypeSwitcher, psFields.piShockUsernameEntry(), psFields.piShockApiKeyEntry(), psFields.logIdentifierField());
    }

    private static @NonNull PishockV1ConfigFields addPishockWebV1ApiSubCategory(ConfigHelper helper, AbstractConfigEntry<@NonNull String> apiTypeSwitcher) {
        helper.startSubCategory(Translation.of("title.pishock-zap.config.api.web_v1"))
            .setDisplayRequirement(() -> DefaultShockBackends.PISHOCK_WEB_V1.equals(apiTypeSwitcher.getValue()));

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api.web_v1.deprecated")
                .withStyle(style -> style.withBold(true).withColor(ChatFormatting.RED)));

        var logIdentifierField = helper.addTextField("api.log_identifier", PishockZapConfig::getLogIdentifier, PishockZapConfig::setLogIdentifier);

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api.web_v1",
                Translation.addLink(
                    Translation.of("description.pishock-zap.config.api.web_v1.api_key_link"),
                    PISHOCK_ACCOUNT_PAGE_URL),
                Translation.addLink(
                    Translation.of("description.pishock-zap.config.api.web_v1.share_codes_link"),
                    PISHOCK_CONTROLLER_PAGE_URL)));

        var piShockUsernameEntry = helper.addTextFieldNoDefault("api.username", PishockZapConfig::getUsername, PishockZapConfig::setUsername);
        var piShockApiKeyEntry = helper.addTextFieldNoDefault("api.api_key", PishockZapConfig::getApiKey, PishockZapConfig::setApiKey);
        StringListListEntry piShockShareCodesField = helper.makeStringListFieldNoDefault("api.share_codes", PishockZapConfig::getShareCodes, PishockZapConfig::setShareCodes, list -> {
            if (!DefaultShockBackends.PISHOCK_WEB_V1.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (list.isEmpty()) return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.empty"));
            return Optional.empty();
        }, shareCode -> {
            if (!DefaultShockBackends.PISHOCK_WEB_V1.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            return isShareCodeInvalid(shareCode);
        });

        {
            var piShockSerialDeviceIdsExt = ListEntryExt.of(piShockShareCodesField);
            if (piShockSerialDeviceIdsExt != null) {
                helper.addActionButton(
                    "api.web_v1.add_my_ids",
                    () -> new PiShockWebApiV1Backend.HttpBackend().probeShareCodes(piShockUsernameEntry.getValue(), piShockApiKeyEntry.getValue()),
                    piShockSerialDeviceIdsExt::pishockZap$addListEntries);
            }
        }

        helper.add(piShockShareCodesField);
        helper.addTextDescription("api.web_v1.disclaimer");

        helper.endSubCategory();
        return new PishockV1ConfigFields(logIdentifierField, piShockUsernameEntry, piShockApiKeyEntry);
    }

    private record PishockV1ConfigFields(StringListEntry logIdentifierField, StringListEntry piShockUsernameEntry,
                                         StringListEntry piShockApiKeyEntry) {
    }

    private static @NonNull DropdownBoxEntry<String> addPishockLocalSerialApiSubCategory(ConfigHelper helper, AbstractConfigEntry<@NonNull String> apiTypeSwitcher) {
        var localApiCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.local"))
            .setDisplayRequirement(() -> DefaultShockBackends.PISHOCK_SERIAL.equals(apiTypeSwitcher.getValue()));

        helper.addTextDescription("api.local");

        var piShockSerialPortEntry = helper.makeDropdown(
            "api.serial_port",
            PishockZapConfig::getSerialPort,
            PishockZapConfig::setSerialPort,
            PiShockSerialBackend.getSerialPorts(),
            Function.identity(),
            Translation::raw);
        helper.add(piShockSerialPortEntry);

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api.local.device_ids",
                Translation.addLink(
                    Translation.of("description.pishock-zap.config.api.local.device_ids.link"),
                    PISHOCK_CONTROLLER_PAGE_URL)));

        var piShockSerialDeviceIdField = helper.makeIntListFieldNoDefault("api.device_ids", PishockZapConfig::getDeviceIds, PishockZapConfig::setDeviceIds, list -> {
            if (!DefaultShockBackends.PISHOCK_SERIAL.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (list.isEmpty())
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.list_empty"));
            return Optional.empty();
        }, id -> {
            if (!DefaultShockBackends.PISHOCK_SERIAL.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (id == null || id < 0)
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.must_be_positive"));
            if (id >= 65536 * 4)
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.too_high"));
            return Optional.empty();
        });

        {
            var piShockSerialDeviceIdsExt = ListEntryExt.of(piShockSerialDeviceIdField);
            if (piShockSerialDeviceIdsExt != null) {
                helper.addActionButton(
                    "api.local.add_my_ids",
                    () -> PiShockSerialBackend.probeDeviceIds(piShockSerialPortEntry.getValue()),
                    piShockSerialDeviceIdsExt::pishockZap$addListEntries);
            }
        }

        helper.add(piShockSerialDeviceIdField);

        helper.endSubCategory();
        return piShockSerialPortEntry;
    }

    private static void addWebHookApiSubCategory(ConfigHelper helper, AbstractConfigEntry<@NonNull String> apiTypeSwitcher) {
        var webhookCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.webhook"))
            .setDisplayRequirement(() -> DefaultShockBackends.WEBHOOK.equals(apiTypeSwitcher.getValue()));

        helper.addTextField("api.custom_webhook_url", PishockZapConfig::getCustomWebhookUrl, PishockZapConfig::setCustomWebhookUrl, url -> {
            if (!DefaultShockBackends.WEBHOOK.equals(apiTypeSwitcher.getValue())) return Optional.empty();
            if (url.isBlank())
                return Optional.of(Translation.of("error.pishock-zap.config.api.custom_webhook_url.empty"));
            try {
                new URI(url);
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

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api.webhook",
                Translation.of("description.pishock-zap.config.api.webhook.payload",
                    Translation.raw("\"" + OpType.SHOCK.name() + "\"").withStyle(style ->
                        TextStyle.withHoverText(style.withColor(ChatFormatting.LIGHT_PURPLE).withUnderlined(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.operation", allOpTypes))),
                    Translation.raw("26").withStyle(style ->
                        TextStyle.withHoverText(style.withColor(ChatFormatting.LIGHT_PURPLE).withUnderlined(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.intensity"))),
                    Translation.raw("1.2").withStyle(style ->
                        TextStyle.withHoverText(style.withColor(ChatFormatting.LIGHT_PURPLE).withUnderlined(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.duration"))),
                    Translation.raw("\"" + ShockDistribution.RANDOM.name() + "\"").withStyle(style ->
                        TextStyle.withHoverText(style.withColor(ChatFormatting.LIGHT_PURPLE).withUnderlined(true),
                            Translation.of("tooltip.pishock-zap.config.api.webhook.payload.distribution", allShockDistributions)))
                ).withStyle(style -> style.withColor(ChatFormatting.GRAY))));

        helper.endSubCategory();
    }

    private static @NonNull StringListEntry addOpenShockWebApiSubCategory(ConfigHelper helper, AbstractConfigEntry<@NonNull String> apiTypeSwitcher, StringListEntry logIdentifierField) {
        var openShockApiCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.openshock"))
            .setDisplayRequirement(() -> DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue()));

        var openShockApiTokenField = helper.addTextField(
            "api.openshock.api_token", PishockZapConfig::getOpenShockApiToken, PishockZapConfig::setOpenShockApiToken, tok -> {
                if (!DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue())) return Optional.empty();
                if (tok.isBlank())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.api_token.empty"));
                return Optional.empty();
            });

        var openShockDeviceIdField = helper.makeStringListFieldNoDefault(
            "api.openshock.device_ids", PishockZapConfig::getOpenShockShockerIds, PishockZapConfig::setOpenShockShockerIds,
            l -> {
                if (!DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue())) return Optional.empty();
                if (l.isEmpty())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.device_ids.list_empty"));
                return Optional.empty();
            },
            id -> {
                if (!DefaultShockBackends.OPENSHOCK_WEB.equals(apiTypeSwitcher.getValue())) return Optional.empty();
                if (id.isBlank())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.device_ids.empty"));
                return Optional.empty();
            }
        );

        var openShockDeviceIdsExt = ListEntryExt.of(openShockDeviceIdField);
        if (openShockDeviceIdsExt != null) {
            helper.addActionButton(
                "api.openshock.add_my_ids",
                () -> OpenShockWebApiBackend.probeDeviceIds(openShockApiTokenField.getValue()),
                openShockDeviceIdsExt::pishockZap$addListEntries);
        }

        helper.add(openShockDeviceIdField);
        helper.add(logIdentifierField);

        helper.addTextDescription("api.openshock.disclaimer");

        helper.endSubCategory();
        return openShockApiTokenField;
    }

    private static void addOpenShockSerialApiSubCategory(ConfigHelper helper, PishockZapConfig config, PishockZapConfig defaultConfig, AbstractConfigEntry<@NonNull String> apiTypeSwitcher, StringListEntry openShockApiTokenField, DropdownBoxEntry<String> piShockSerialPortEntry) {
        var openShockSerialApiCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.openshock.serial"))
            .setDisplayRequirement(() -> DefaultShockBackends.OPENSHOCK_SERIAL.equals(apiTypeSwitcher.getValue()));

        helper.addTextDescription("api.openshock.serial");

        var openShockDeviceListEntry = NestedList.<ShockDevice, MultiElementListEntry<ShockDevice>>builder()
            .setTitle(Translation.of("title.pishock-zap.config.api.openshock.serial.devices"))
            .setInitialValue(config.getOpenShockSerialDevices())
            .setTooltipSupplier(ClothUtil.supply(Translation.of("tooltip.pishock-zap.config.api.openshock.serial.devices")))
            .setSaveConsumer(config::setOpenShockSerialDevices)
            .setDefaultValueSupplier(defaultConfig::getOpenShockSerialDevices)
            .setResetButtonKey(helper.getResetButtonKey())
            .setDefaultNewEntryValue(new ShockDevice(ShockCollarModel.CAIXIANLIN, 0))
            .setWidgetCreator((elem, widget) -> new Arity2StructEntry<>(
                Translation.of("title.pishock-zap.config.api.openshock.serial.devices.entry"),
                ShockDevice::new,
                helper.makeOpenShockCollarModelDropdown("api.openshock.serial.devices.entry.model", elem.model()),
                helper.makeIntField("api.openshock.serial.devices.entry.id", elem.id())))
            .build();

        addOpenShockSerialDeviceFetchSubCategory(helper, openShockApiTokenField, openShockDeviceListEntry);

        helper.add(piShockSerialPortEntry);
        helper.add(openShockDeviceListEntry);

        helper.endSubCategory();
    }

    private static void addOpenShockSerialDeviceFetchSubCategory(ConfigHelper helper, StringListEntry openShockApiTokenField, NestedListListEntry<ShockDevice, MultiElementListEntry<ShockDevice>> openShockDeviceListEntry) {
        var openShockDeviceListEntryExt = ListEntryExt.of(openShockDeviceListEntry);
        if (openShockDeviceListEntryExt != null) {
            helper.startSubCategory(Translation.of("title.pishock-zap.config.api.openshock.serial.fetch"));

            helper.add(openShockApiTokenField);
            helper.addActionButton("api.openshock.serial.fetch.button",
                () -> OpenShockWebApiBackend.probeDevices(openShockApiTokenField.getValue()),
                result -> result.stream().map(s -> new ShockDevice(s.model(), s.rfId()))
                    .forEachOrdered(openShockDeviceListEntryExt::pishockZap$addListEntry));

            helper.endSubCategory();
        }
    }

    @SuppressWarnings("deprecation") // TextFieldListEntry.setValue is deprecated
    private static void addPishockWebSocketApiSubCategory(ConfigHelper helper, PishockZapConfig config, PishockZapConfig defaultConfig, AbstractConfigEntry<@NonNull String> apiTypeSwitcher, StringListEntry piShockUsernameEntry, StringListEntry piShockApiKeyEntry, StringListEntry logIdentifierField) {
        var piShockWebSocketApiCategory = helper
            .startSubCategory("api.pishock.websocket")
            .setDisplayRequirement(() -> DefaultShockBackends.PISHOCK_WEBSOCKET.equals(apiTypeSwitcher.getValue()));

        helper.add(piShockUsernameEntry);
        helper.add(piShockApiKeyEntry);
        helper.add(logIdentifierField);

        var hubDeviceIdListEntry = NestedList.<Pair<Integer, IntList>, MultiElementListEntry<Pair<Integer, IntList>>>builder()
            .setTitle(Translation.of("title.pishock-zap.config.api.pishock.websocket.devices"))
            .setInitialValue(hubShockerMapToList(config.getPsHubShockers()))
            .setTooltipSupplier(ClothUtil.supply(Translation.of("tooltip.pishock-zap.config.api.pishock.websocket.devices")))
            .setSaveConsumer(list -> {
                var result = new Int2ObjectArrayMap<IntList>();
                for (var pair : list) {
                    result.put(pair.getLeft().intValue(), pair.getRight());
                }
                config.setPsHubShockers(result);
            })
            .setDefaultValueSupplier(() -> hubShockerMapToList(defaultConfig.getPsHubShockers()))
            .setResetButtonKey(helper.getResetButtonKey())
            .setDefaultNewEntryValue(Pair.of(0, new IntArrayList(new int[]{0})))
            .setWidgetCreator((elem, widget) -> new Arity2StructEntry<>(
                Translation.of("title.pishock-zap.config.api.pishock.websocket.devices.entry"),
                (a, b) -> Pair.of(a, new IntArrayList(b)),
                helper.makeIntField("api.pishock.websocket.devices.entry.id", elem.getLeft()),
                helper.makeIntListField("api.pishock.websocket.devices.entry.devices", elem.getRight())))
            .build();

        var websocketUserIdEntry = helper.makeIntField("api.pishock.websocket.user_id",
            PishockZapConfig::getPsUserId,
            PishockZapConfig::setPsUserId,
            value -> Optional.empty());

        helper.addActionButton("api.pishock.websocket.fetch_ids",
            () -> {
                var backend = new PiShockWebApiV1Backend.HttpBackend();
                var apiKey = piShockApiKeyEntry.getValue();
                return backend.getUserProfile(piShockUsernameEntry.getValue(), apiKey)
                    .thenComposeAsync(profile ->
                        backend.getUserDevices(profile.userId, apiKey)
                            .thenApply(devices -> Pair.of(profile, devices)));
            },
            result -> {
                var profile = result.getLeft();
                var devices = result.getRight();

                websocketUserIdEntry.setValue(Integer.toString(profile.userId));

                var ext = ListEntryExt.of(hubDeviceIdListEntry);
                if (ext != null) {
                    ext.pishockZap$addListEntries(devices.stream()
                        .map(d -> Pair.<Integer, IntList>of(d.clientId, IntArrayList.wrap(d.shockers.stream()
                            .mapToInt(s -> s.shockerId).toArray()))).toList());
                }
            });

        helper.add(websocketUserIdEntry);
        helper.add(hubDeviceIdListEntry);

        helper.addTextDescription("api.websocket.disclaimer");

        helper.endSubCategory();
    }

    private static @NonNull List<Pair<Integer, IntList>> hubShockerMapToList(Int2ObjectMap<IntList> psHubShockers) {
        return psHubShockers.int2ObjectEntrySet().stream()
            .map(hub -> Pair.of(hub.getIntKey(), hub.getValue()))
            .toList();
    }

    private static @NonNull Optional<Component> isShareCodeInvalid(@NonNull String shareCode) {
        if (shareCode.isBlank())
            return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.entry.empty"));
        if (shareCode.length() < 10 || !shareCode.matches("[0-9A-F]+")) {
            return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.entry.invalid"));
        }
        return Optional.empty();
    }

    @Override
    public @NonNull ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PishockZapModConfigMenu::createConfigScreen;
    }
}
