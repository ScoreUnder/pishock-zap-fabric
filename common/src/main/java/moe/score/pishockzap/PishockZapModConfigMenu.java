package moe.score.pishockzap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.NonNull;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackendRegistry;
import moe.score.pishockzap.backend.impls.OpenShockWebApiBackend;
import moe.score.pishockzap.backend.impls.PiShockSerialBackend;
import moe.score.pishockzap.backend.impls.PiShockWebApiV1Backend;
import moe.score.pishockzap.backend.model.openshock.ShockCollarModel;
import moe.score.pishockzap.backend.model.openshock.ShockDevice;
import moe.score.pishockzap.compat.*;
import moe.score.pishockzap.compat.clothconfig.BetterMultiElementListEntry;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.mixin.pool.ListEntryExt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import java.util.stream.Collectors;

import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_DURATION;
import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_INTENSITY;

@SuppressWarnings("unused")
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
        var helper = new Helper(config, defaultConfig, configBuilder);

        addGeneralCategory(helper);
        addLimitsCategory(helper);
        addDebounceCategory(helper);
        addApiCategory(helper, config, defaultConfig);

        configBuilder.setSavingRunnable(mod::saveConfig);
        return configBuilder.build();
    }

    private static void addGeneralCategory(Helper helper) {
        helper.startCategory("general");

        helper.addBooleanSwitch("general.enabled", PishockZapConfig::isEnabled, PishockZapConfig::setEnabled);
        helper.addBooleanSwitch("general.vibration_only", PishockZapConfig::isVibrationOnly, PishockZapConfig::setVibrationOnly);
        helper.addBooleanSwitch("general.shock_on_health", PishockZapConfig::isShockOnHealth, PishockZapConfig::setShockOnHealth);
        helper.addBooleanSwitch("general.fractional_damage", PishockZapConfig::isFractionalDamage, PishockZapConfig::setFractionalDamage);
    }

    private static void addLimitsCategory(Helper helper) {
        helper.startCategory("limits");

        addShockLimitsSubCategory(helper);
        addDamageThresholdsSubCategory(helper);
        addShockOnDeathSubCategory(helper);
    }

    private static void addShockLimitsSubCategory(Helper helper) {
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

    private static void addDamageThresholdsSubCategory(Helper helper) {
        helper.startSubCategory(Translation.of("title.pishock-zap.config.limits.damage_thresholds"));

        helper.addFloatSlider("limits.vibration_threshold", "hp", PishockZapConfig::getVibrationThreshold, PishockZapConfig::setVibrationThreshold, 0, 1, 100, 100);
        helper.addFloatSlider("limits.min_damage", "hp", PishockZapConfig::getMinDamage, PishockZapConfig::setMinDamage, 0, 1, 100, 100);
        helper.addFloatSlider("limits.max_damage", "hp", PishockZapConfig::getMaxDamage, PishockZapConfig::setMaxDamage, 0, 1, 100, 100);

        helper.endSubCategory();
    }

    private static void addShockOnDeathSubCategory(Helper helper) {
        helper.startSubCategory(Translation.of("title.pishock-zap.config.limits.shock_on_death_category"));

        helper.addBooleanSwitch("general.shock_on_death", PishockZapConfig::isShockOnDeath, PishockZapConfig::setShockOnDeath);
        helper.addIntSlider("limits.shock_intensity_death", "intensity", PishockZapConfig::getShockIntensityDeath, PishockZapConfig::setShockIntensityDeath, 1, PISHOCK_MAX_INTENSITY);
        helper.addFloatSlider("limits.shock_duration_death", "duration", PishockZapConfig::getShockDurationDeath, PishockZapConfig::setShockDurationDeath, 0.1f, PISHOCK_MAX_DURATION);
        helper.add(helper.makeShockDistributionDropdown("limits.shock_distribution_death", PishockZapConfig::getShockDistributionDeath, PishockZapConfig::setShockDistributionDeath));

        helper.endSubCategory();
    }

    private static void addDebounceCategory(Helper helper) {
        helper.startCategory("debounce");

        helper.addFloatSlider("debounce.debounce_time", "time_interval", PishockZapConfig::getDebounceTime, PishockZapConfig::setDebounceTime, 0.1f, 60f);
        helper.addBooleanSwitch("debounce.accumulate_duration", PishockZapConfig::isAccumulateDuration, PishockZapConfig::setAccumulateDuration);
        helper.addBooleanSwitch("debounce.accumulate_intensity", PishockZapConfig::isAccumulateIntensity, PishockZapConfig::setAccumulateIntensity);
        helper.addBooleanSwitch("debounce.queue_different", PishockZapConfig::isQueueDifferent, PishockZapConfig::setQueueDifferent);
    }

    private static void addApiCategory(Helper helper, PishockZapConfig config, PishockZapConfig defaultConfig) {
        helper.startCategory("api");

        var apiTypeSwitcher = helper.makeSelector(
            "api_type",
            PishockZapConfig::getApiType,
            PishockZapConfig::setApiType,
            ShockBackendRegistry.getAllBackendIds(),
            value -> Translation.of(ShockBackendRegistry.getTranslationKey(value)));

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

    private static @NonNull PishockV1ConfigFields addPishockWebV1ApiSubCategory(Helper helper, SelectionListEntry<@NonNull String> apiTypeSwitcher) {
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
        PishockV1ConfigFields psFields = new PishockV1ConfigFields(logIdentifierField, piShockUsernameEntry, piShockApiKeyEntry);
        return psFields;
    }

    private record PishockV1ConfigFields(StringListEntry logIdentifierField, StringListEntry piShockUsernameEntry,
                                         StringListEntry piShockApiKeyEntry) {
    }

    private static @NonNull DropdownBoxEntry<String> addPishockLocalSerialApiSubCategory(Helper helper, SelectionListEntry<@NonNull String> apiTypeSwitcher) {
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

    private static void addWebHookApiSubCategory(Helper helper, SelectionListEntry<@NonNull String> apiTypeSwitcher) {
        var webhookCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.webhook"))
            .setDisplayRequirement(() -> DefaultShockBackends.WEBHOOK.equals(apiTypeSwitcher.getValue()));

        helper.addTextField("api.custom_webhook_url", PishockZapConfig::getCustomWebhookUrl, PishockZapConfig::setCustomWebhookUrl, url -> {
            if (!DefaultShockBackends.WEBHOOK.equals(apiTypeSwitcher.getValue())) return Optional.empty();
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

    private static @NonNull StringListEntry addOpenShockWebApiSubCategory(Helper helper, SelectionListEntry<@NonNull String> apiTypeSwitcher, StringListEntry logIdentifierField) {
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

    private static void addOpenShockSerialApiSubCategory(Helper helper, PishockZapConfig config, PishockZapConfig defaultConfig, SelectionListEntry<@NonNull String> apiTypeSwitcher, StringListEntry openShockApiTokenField, DropdownBoxEntry<String> piShockSerialPortEntry) {
        var openShockSerialApiCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.openshock.serial"))
            .setDisplayRequirement(() -> DefaultShockBackends.OPENSHOCK_SERIAL.equals(apiTypeSwitcher.getValue()));

        helper.addTextDescription("api.openshock.serial");

        var openShockDeviceListEntry = new NestedListListEntry<ShockDevice, MultiElementListEntry<ShockDevice>>(
            Translation.of("title.pishock-zap.config.api.openshock.serial.devices"),
            config.getOpenShockSerialDevices(),
            true,
            () -> Optional.of(new Component[]{Translation.of("tooltip.pishock-zap.config.api.openshock.serial.devices")}),
            config::setOpenShockSerialDevices,
            defaultConfig::getOpenShockSerialDevices,
            helper.getResetButtonKey(),
            true,
            true,
            (elem, nestedListListEntry) -> {
                if (elem == null) {
                    elem = new ShockDevice(ShockCollarModel.CAIXIANLIN, 0);
                }
                var deviceId = helper.makeIntField("api.openshock.serial.devices.entry.id", elem.id());
                var deviceModel = helper.makeOpenShockCollarModelDropdown("api.openshock.serial.devices.entry.model", elem.model());
                return new BetterMultiElementListEntry<>(
                    Translation.of("title.pishock-zap.config.api.openshock.serial.devices.entry"),
                    () -> new ShockDevice(deviceModel.getValue(), deviceId.getValue()),
                    List.of(deviceId, deviceModel));
            }
        );

        addOpenShockSerialDeviceFetchSubCategory(helper, openShockApiTokenField, openShockDeviceListEntry);

        helper.add(piShockSerialPortEntry);
        helper.add(openShockDeviceListEntry);

        helper.endSubCategory();
    }

    private static void addOpenShockSerialDeviceFetchSubCategory(Helper helper, StringListEntry openShockApiTokenField, NestedListListEntry<ShockDevice, MultiElementListEntry<ShockDevice>> openShockDeviceListEntry) {
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

    private static void addPishockWebSocketApiSubCategory(Helper helper, PishockZapConfig config, PishockZapConfig defaultConfig, SelectionListEntry<@NonNull String> apiTypeSwitcher, StringListEntry piShockUsernameEntry, StringListEntry piShockApiKeyEntry, StringListEntry logIdentifierField) {
        var piShockWebSocketApiCategory = helper
            .startSubCategory("api.pishock.websocket")
            .setDisplayRequirement(() -> DefaultShockBackends.PISHOCK_WEBSOCKET.equals(apiTypeSwitcher.getValue()));

        helper.add(piShockUsernameEntry);
        helper.add(piShockApiKeyEntry);
        helper.add(logIdentifierField);

        var hubDeviceIdListEntry = new NestedListListEntry<Pair<Integer, IntList>, MultiElementListEntry<Pair<Integer, IntList>>>(
            Translation.of("title.pishock-zap.config.api.pishock.websocket.devices"),
            hubShockerMapToList(config.getPsHubShockers()),
            true,
            () -> Optional.of(new Component[]{Translation.of("tooltip.pishock-zap.config.api.pishock.websocket.devices")}),
            list -> {
                var result = new Int2ObjectArrayMap<IntList>();
                for (var pair : list) {
                    result.put(pair.getLeft().intValue(), pair.getRight());
                }
                config.setPsHubShockers(result);
            },
            () -> hubShockerMapToList(defaultConfig.getPsHubShockers()),
            helper.getResetButtonKey(),
            true,
            true,
            (elem, nestedListListEntry) -> {
                if (elem == null) {
                    elem = Pair.of(0, new IntArrayList(new int[]{0}));
                }
                var hubId = helper.makeIntField("api.pishock.websocket.devices.entry.id", elem.getLeft());
                var shockersList = helper.makeIntListField("api.pishock.websocket.devices.entry.devices", elem.getRight());
                return new BetterMultiElementListEntry<>(
                    Translation.of("title.pishock-zap.config.api.pishock.websocket.devices.entry"),
                    () -> Pair.of(hubId.getValue(), new IntArrayList(shockersList.getValue())),
                    List.of(hubId, shockersList));
            }
        );

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

                //noinspection deprecation
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

    private static class Helper {
        private final PishockZapConfig config;
        private final PishockZapConfig defaultConfig;
        private final ConfigBuilder configBuilder;
        private final ConfigEntryBuilder entryBuilder;
        private Consumer<AbstractConfigListEntry<?>> addEntry;
        private final List<Consumer<AbstractConfigListEntry<?>>> addEntryStack = new ArrayList<>();
        private final List<BuilderCompat.SubCategoryBuilderCompat> subCategoryStack = new ArrayList<>();

        public Helper(PishockZapConfig config, PishockZapConfig defaultConfig, ConfigBuilder configBuilder) {
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

        public ConfigCategory startCategory(Component title) {
            if (!subCategoryStack.isEmpty()) throw new IllegalStateException();
            var cat = configBuilder.getOrCreateCategory(title);
            setCategory(cat);
            return cat;
        }

        public ConfigCategory startCategory(String keyPart) {
            return startCategory(Translation.of("title.pishock-zap.config." + keyPart));
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
}
