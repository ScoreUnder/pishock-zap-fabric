package moe.score.pishockzap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.FieldDefaults;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
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
import moe.score.pishockzap.compat.clothconfig.ConfigHelper;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.mixin.pool.ListEntryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_DURATION;
import static moe.score.pishockzap.backend.PiShockUtils.PISHOCK_MAX_INTENSITY;

@SuppressWarnings("unused")
@ApiStatus.Internal
@ExtensionMethod(ListEntryUtil.class)
public class PishockZapModConfigMenu implements ModMenuApi {
    public static final String PISHOCK_ACCOUNT_PAGE_URL = "https://login.pishock.com/account";
    private static final String PISHOCK_CONTROLLER_PAGE_URL = "https://pishock.com/#/control";

    private static @NonNull Screen createConfigScreen(Screen parent) {
        var mod = Objects.requireNonNull(PishockZapMod.getInstance(), "PishockZapMod instance is null");
        var config = mod.getConfig();

        var defaultConfig = new PishockZapConfig();
        var helper = new ConfigHelper(config, defaultConfig, parent);

        addGeneralCategory(helper);
        addLimitsCategory(helper);
        addDebounceCategory(helper);
        addApiCategory(helper);

        return helper.buildScreen(mod::saveConfig);
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

    private static void addApiCategory(ConfigHelper helper) {
        helper.startCategory("api");

        var apiTypeSwitcher = helper.addSelector(
            "api_type",
            PishockZapConfig::getApiType,
            PishockZapConfig::setApiType,
            Arrays.stream(ShockBackendRegistry.getAllBackendIds())
                .sorted(Comparator.comparingInt(PishockZapModConfigMenu::specialOrderingKey).thenComparing(a -> a))
                .toArray(String[]::new),
            PishockZapModConfigMenu::getApiTypeTranslation);
        Supplier<String> getApiType = apiTypeSwitcher::getValue;

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api_type",
                Translation.of("enum.pishock-zap.config.api_type.websocket").withStyle(style -> style.withBold(true))));

        var pishockAccountDetails = addPishockAccountSubCategory(helper, getApiType);
        var openShockAccountDetails = addOpenShockAccountSubCategory(helper, getApiType);
        var loggingDetails = addLoggingSubCategory(helper, getApiType);
        var serialPortDetails = addSerialPortSubCategory(helper, getApiType);

        addPishockWebV1ApiSubCategory(helper, getApiType, pishockAccountDetails, loggingDetails);
        addPishockLocalSerialApiSubCategory(helper, getApiType, serialPortDetails);
        addWebHookApiSubCategory(helper, getApiType);
        addOpenShockWebApiSubCategory(helper, getApiType, openShockAccountDetails, loggingDetails);
        addOpenShockSerialApiSubCategory(helper, getApiType, openShockAccountDetails, serialPortDetails);
        addPishockWebSocketApiSubCategory(helper, getApiType, pishockAccountDetails, loggingDetails);
    }

    private static LoggingBackendDetails addLoggingSubCategory(ConfigHelper helper, Supplier<String> apiType) {
        var showForBackends = new HashSet<>();
        var loggingCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.logging"))
            .setDisplayRequirement(() -> showForBackends.contains(apiType.get()));

        var logIdentifierField = helper.addTextField("api.logging.log_identifier", PishockZapConfig::getLogIdentifier, PishockZapConfig::setLogIdentifier);

        helper.endSubCategory();

        return new LoggingBackendDetails(logIdentifierField::getValue, showForBackends::add);
    }

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    private static class LoggingBackendDetails {
        Supplier<String> logIdentifier;
        Consumer<String> showForBackend;

        public String logIdentifier() {
            return logIdentifier.get();
        }

        public void showForBackend(String backend) {
            showForBackend.accept(backend);
        }
    }

    private static PishockAccountDetails addPishockAccountSubCategory(ConfigHelper helper, Supplier<String> apiType) {
        var showForBackends = new HashSet<>();
        var commonCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.pishock.account"))
            .setDisplayRequirement(() -> showForBackends.contains(apiType.get()));

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api.pishock.account",
                Translation.addLink(
                    Translation.of("description.pishock-zap.config.api.pishock.account.api_key_link"),
                    PISHOCK_ACCOUNT_PAGE_URL),
                Translation.addLink(
                    Translation.of("description.pishock-zap.config.api.pishock.account.share_codes_link"),
                    PISHOCK_CONTROLLER_PAGE_URL)));

        var piShockUsernameEntry = helper.addTextFieldNoDefault("api.pishock.account.username", PishockZapConfig::getUsername, PishockZapConfig::setUsername);
        var piShockApiKeyEntry = helper.addTextFieldNoDefault("api.pishock.account.api_key", PishockZapConfig::getApiKey, PishockZapConfig::setApiKey);

        helper.endSubCategory();

        return new PishockAccountDetails(piShockUsernameEntry::getValue, piShockApiKeyEntry::getValue, showForBackends::add);
    }

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    private static class PishockAccountDetails {
        Supplier<String> username;
        Supplier<String> apiKey;
        Consumer<String> showForBackend;

        public String username() {
            return username.get();
        }

        public String apiKey() {
            return apiKey.get();
        }

        public void showForBackend(String backend) {
            showForBackend.accept(backend);
        }
    }

    private static void addPishockWebV1ApiSubCategory(ConfigHelper helper, Supplier<String> apiType, PishockAccountDetails accountDetails, LoggingBackendDetails loggingDetails) {
        var thisBackend = DefaultShockBackends.PISHOCK_WEB_V1;
        accountDetails.showForBackend(thisBackend);
        loggingDetails.showForBackend(thisBackend);

        helper.startSubCategory(Translation.of("title.pishock-zap.config.api.web_v1"))
            .setDisplayRequirement(() -> thisBackend.equals(apiType.get()));

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api.web_v1.deprecated")
                .withStyle(style -> style.withBold(true).withColor(ChatFormatting.RED)));

        StringListListEntry piShockShareCodesField = helper.makeStringListFieldNoDefault("api.share_codes", PishockZapConfig::getShareCodes, PishockZapConfig::setShareCodes, list -> {
            if (!thisBackend.equals(apiType.get())) return Optional.empty();
            if (list.isEmpty()) return Optional.of(Translation.of("error.pishock-zap.config.api.share_codes.empty"));
            return Optional.empty();
        }, shareCode -> {
            if (!thisBackend.equals(apiType.get())) return Optional.empty();
            return isShareCodeInvalid(shareCode);
        });

        ListEntryUtil.withExtensions(piShockShareCodesField, list ->
            helper.addActionButton(
                "api.web_v1.add_my_ids",
                () -> new PiShockWebApiV1Backend.HttpBackend().probeShareCodes(accountDetails.username(), accountDetails.apiKey()),
                v -> list.replaceValues(v)));

        helper.add(piShockShareCodesField);
        helper.addTextDescription("api.web_v1.disclaimer");

        helper.endSubCategory();
    }

    private static SerialPortDetails addSerialPortSubCategory(ConfigHelper helper, Supplier<String> apiType) {
        var showForBackends = new HashSet<>();
        var category = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.serial_port"))
            .setDisplayRequirement(() -> showForBackends.contains(apiType.get()));

        var serialPortEntry = helper.makeDropdown(
            "api.serial_port",
            PishockZapConfig::getSerialPort,
            PishockZapConfig::setSerialPort,
            PiShockSerialBackend.getSerialPorts(),
            Function.identity(),
            Translation::raw);
        helper.add(serialPortEntry);

        helper.endSubCategory();

        return new SerialPortDetails(serialPortEntry::getValue, showForBackends::add);
    }

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    private static class SerialPortDetails {
        Supplier<String> serialPort;
        Consumer<String> showForBackend;

        public String serialPort() {
            return serialPort.get();
        }

        public void showForBackend(String backend) {
            showForBackend.accept(backend);
        }
    }

    private static void addPishockLocalSerialApiSubCategory(ConfigHelper helper, Supplier<String> apiType, SerialPortDetails serialPortDetails) {
        var thisBackend = DefaultShockBackends.PISHOCK_SERIAL;
        serialPortDetails.showForBackend(thisBackend);

        var localApiCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.local"))
            .setDisplayRequirement(() -> thisBackend.equals(apiType.get()));

        helper.addTextDescription("api.local");

        helper.addTextDescription(
            Translation.of("description.pishock-zap.config.api.local.device_ids",
                Translation.addLink(
                    Translation.of("description.pishock-zap.config.api.local.device_ids.link"),
                    PISHOCK_CONTROLLER_PAGE_URL)));

        var piShockSerialDeviceIdField = helper.makeIntListFieldNoDefault("api.device_ids", PishockZapConfig::getDeviceIds, PishockZapConfig::setDeviceIds, list -> {
            if (!thisBackend.equals(apiType.get())) return Optional.empty();
            if (list.isEmpty())
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.list_empty"));
            return Optional.empty();
        }, id -> {
            if (!thisBackend.equals(apiType.get())) return Optional.empty();
            if (id == null || id < 0)
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.must_be_positive"));
            if (id >= 65536 * 4)
                return Optional.of(Translation.of("error.pishock-zap.config.api.local.device_ids.too_high"));
            return Optional.empty();
        });

        ListEntryUtil.withExtensions(piShockSerialDeviceIdField, list ->
            helper.addActionButton(
                "api.local.add_my_ids",
                () -> PiShockSerialBackend.probeDeviceIds(serialPortDetails.serialPort()),
                values -> list.replaceValues(values)));

        helper.add(piShockSerialDeviceIdField);

        helper.endSubCategory();
    }

    private static void addWebHookApiSubCategory(ConfigHelper helper, Supplier<String> apiType) {
        var webhookCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.webhook"))
            .setDisplayRequirement(() -> DefaultShockBackends.WEBHOOK.equals(apiType.get()));

        helper.addTextField("api.custom_webhook_url", PishockZapConfig::getCustomWebhookUrl, PishockZapConfig::setCustomWebhookUrl, url -> {
            if (!DefaultShockBackends.WEBHOOK.equals(apiType.get())) return Optional.empty();
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

    private static OpenShockAccountDetails addOpenShockAccountSubCategory(ConfigHelper helper, Supplier<String> apiType) {
        var showForBackends = new HashSet<>();
        var category = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.openshock.account"))
            .setDisplayRequirement(() -> showForBackends.contains(apiType.get()));

        var openShockApiTokenField = helper.addTextFieldNoDefault(
            "api.openshock.account.api_token", PishockZapConfig::getOpenShockApiToken, PishockZapConfig::setOpenShockApiToken, tok -> {
                if (!showForBackends.contains(apiType.get())) return Optional.empty();
                if (tok.isBlank())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.account.api_token.empty"));
                return Optional.empty();
            });

        helper.endSubCategory();

        return new OpenShockAccountDetails(openShockApiTokenField::getValue, showForBackends::add);
    }

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    private static class OpenShockAccountDetails {
        Supplier<String> apiToken;
        Consumer<String> showForBackend;

        public String apiToken() {
            return apiToken.get();
        }

        public void showForBackend(String backend) {
            showForBackend.accept(backend);
        }
    }

    private static void addOpenShockWebApiSubCategory(ConfigHelper helper, Supplier<String> apiType, OpenShockAccountDetails accountDetails, LoggingBackendDetails loggingDetails) {
        var thisBackend = DefaultShockBackends.OPENSHOCK_WEB;
        accountDetails.showForBackend(thisBackend);
        loggingDetails.showForBackend(thisBackend);

        var openShockApiCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.openshock"))
            .setDisplayRequirement(() -> thisBackend.equals(apiType.get()));

        var openShockDeviceIdField = helper.makeStringListFieldNoDefault(
            "api.openshock.device_ids", PishockZapConfig::getOpenShockShockerIds, PishockZapConfig::setOpenShockShockerIds,
            l -> {
                if (!thisBackend.equals(apiType.get())) return Optional.empty();
                if (l.isEmpty())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.device_ids.list_empty"));
                return Optional.empty();
            },
            id -> {
                if (!thisBackend.equals(apiType.get())) return Optional.empty();
                if (id.isBlank())
                    return Optional.of(Translation.of("error.pishock-zap.config.api.openshock.device_ids.empty"));
                return Optional.empty();
            }
        );

        ListEntryUtil.withExtensions(openShockDeviceIdField, list ->
            helper.addActionButton(
                "api.openshock.add_my_ids",
                () -> OpenShockWebApiBackend.probeDeviceIds(accountDetails.apiToken()),
                values -> list.replaceValues(values)));

        helper.add(openShockDeviceIdField);

        helper.addTextDescription("api.openshock.disclaimer");

        helper.endSubCategory();
    }

    private static void addOpenShockSerialApiSubCategory(ConfigHelper helper, Supplier<String> apiType, OpenShockAccountDetails accountDetails, SerialPortDetails serialPortDetails) {
        var thisBackend = DefaultShockBackends.OPENSHOCK_SERIAL;
        serialPortDetails.showForBackend(thisBackend);
        // accountDetails.showForBackend(thisBackend); -- done halfway down this function when we know the list extension mixin is there, since the account details are only needed for fetching device IDs automatically

        var openShockSerialApiCategory = helper.startSubCategory(Translation.of("title.pishock-zap.config.api.openshock.serial"))
            .setDisplayRequirement(() -> thisBackend.equals(apiType.get()));

        helper.addTextDescription("api.openshock.serial");

        var openShockDeviceListEntry = helper.makeNestedList(
            "api.openshock.serial.devices",
            PishockZapConfig::getOpenShockSerialDevices,
            PishockZapConfig::setOpenShockSerialDevices,
            new ShockDevice(ShockCollarModel.CAIXIANLIN, 0),
            (elem, widget) -> new Arity2StructEntry<>(
                Translation.of("title.pishock-zap.config.api.openshock.serial.devices.entry"),
                ShockDevice::new,
                helper.makeOpenShockCollarModelDropdown("api.openshock.serial.devices.entry.model", elem.model()),
                helper.makeIntField("api.openshock.serial.devices.entry.id", elem.id())));

        ListEntryUtil.withExtensions(openShockDeviceListEntry, list -> {
            accountDetails.showForBackend(thisBackend);

            helper.addActionButton("api.openshock.serial.fetch.button",
                () -> OpenShockWebApiBackend.probeDevices(accountDetails.apiToken()),
                result -> {
                    if (!result.isEmpty()) list.pishockZap$clear();
                    result.stream().map(s -> new ShockDevice(s.model(), s.rfId()))
                        .forEachOrdered(list::pishockZap$addListEntry);
                });
        });

        helper.add(openShockDeviceListEntry);

        helper.endSubCategory();
    }

    @SuppressWarnings("deprecation") // TextFieldListEntry.setValue is deprecated
    private static void addPishockWebSocketApiSubCategory(ConfigHelper helper, Supplier<String> apiType, PishockAccountDetails accountDetails, LoggingBackendDetails loggingDetails) {
        var thisBackend = DefaultShockBackends.PISHOCK_WEBSOCKET;
        accountDetails.showForBackend(thisBackend);
        loggingDetails.showForBackend(thisBackend);

        var piShockWebSocketApiCategory = helper
            .startSubCategory("api.pishock.websocket")
            .setDisplayRequirement(() -> thisBackend.equals(apiType.get()));

        var hubDeviceIdListEntry = helper.makeNestedList(
            "api.pishock.websocket.devices",
            config -> hubShockerMapToList(config.getPsHubShockers()),
            (config, list) -> {
                var result = new Int2ObjectArrayMap<IntList>();
                for (var pair : list) {
                    result.put(pair.getLeft().intValue(), pair.getRight());
                }
                config.setPsHubShockers(result);
            },
            Pair.of(0, new IntArrayList(new int[]{0})),
            (elem, widget) -> new Arity2StructEntry<>(
                Translation.of("title.pishock-zap.config.api.pishock.websocket.devices.entry"),
                (a, b) -> Pair.of(a, new IntArrayList(b)),
                helper.makeIntField("api.pishock.websocket.devices.entry.id", elem.getLeft()),
                helper.makeIntListField("api.pishock.websocket.devices.entry.devices", elem.getRight()))
        );

        var websocketUserIdEntry = helper.makeIntField("api.pishock.websocket.user_id",
            PishockZapConfig::getPsUserId,
            PishockZapConfig::setPsUserId,
            value -> Optional.empty());

        helper.addActionButton("api.pishock.websocket.fetch_ids",
            () -> {
                var backend = new PiShockWebApiV1Backend.HttpBackend();
                var apiKey = accountDetails.apiKey();
                return backend.getUserProfile(accountDetails.username(), apiKey)
                    .thenComposeAsync(profile ->
                        backend.getUserDevices(profile.userId, apiKey)
                            .thenApply(devices -> Pair.of(profile, devices)));
            },
            result -> {
                var profile = result.getLeft();
                var devices = result.getRight();

                websocketUserIdEntry.setValue(Integer.toString(profile.userId));

                ListEntryUtil.withExtensions(hubDeviceIdListEntry, list -> {
                    if (devices.isEmpty()) return;
                    list.replaceValues(devices.stream()
                        .map(d -> Pair.<Integer, IntList>of(d.clientId, IntArrayList.wrap(d.shockers.stream()
                            .mapToInt(s -> s.shockerId).toArray()))).toList());
                });
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
