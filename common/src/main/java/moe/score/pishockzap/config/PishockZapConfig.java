package moe.score.pishockzap.config;

import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.DefaultShockBackends;
import moe.score.pishockzap.annotation.InternalMembers;
import moe.score.pishockzap.backend.model.openshock.ShockDevice;
import moe.score.pishockzap.config.internal.PiShockWebSocketApiConfig;
import moe.score.pishockzap.util.Gsons;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the full configuration of the Pishock-Zap mod. This is a live object (modified in-place), not a snapshot.
 * <p>
 * The internals of this class are not meant for API consumers and will change without warning.
 */
@Data
@ApiStatus.Experimental
@InternalMembers
@Slf4j(topic = Constants.NAME)
public class PishockZapConfig implements PiShockWebSocketApiConfig {
    static final @NonNull String CONFIG_VERSION_KEY = "CONFIG_VERSION_DO_NOT_EDIT";
    static final int CONFIG_VERSION = 3;

    /// Whether the mod is enabled at all
    private boolean enabled = false;
    /// Whether shocks should be sent as vibrations instead
    private boolean vibrationOnly = false;
    /// Whether to shock/vibrate when the player dies
    private boolean shockOnDeath = true;
    /// Whether to shock/vibrate based on the player's health rather than damage
    private boolean shockOnHealth = false;
    /// Whether to allow fractional half-heart damage
    private boolean fractionalDamage = false;

    /// The duration per shock/vibration
    private float duration = 0.4f;
    /// The maximum duration per shock/vibration, if it varies e.g. based on debouncing
    private float maxDuration = 10.0f;
    /// The threshold to swap from vibration to shock
    private float vibrationThreshold = 0.0f;
    /// The damage value corresponding to the least intense shock/vibration
    private float minDamage = 0.05f;
    /// The damage value corresponding to the most intense shock
    private float maxDamage = 1.0f;
    /// The minimum intensity of a vibration
    private int vibrationIntensityMin = 20;
    /// The maximum intensity of a vibration
    private int vibrationIntensityMax = 100;
    /// The minimum intensity of a shock
    private int shockIntensityMin = 5;
    /// The maximum intensity of a shock
    private int shockIntensityMax = 60;
    /// The intensity of a shock when the player dies
    private int shockIntensityDeath = 75;
    /// The duration of a shock when the player dies
    private float shockDurationDeath = 5.0f;
    /// The distribution of shocks when the player takes damage
    private @NonNull ShockDistribution shockDistribution = ShockDistribution.ROUND_ROBIN;
    /// The distribution of shocks when the player dies
    private @NonNull ShockDistribution shockDistributionDeath = ShockDistribution.ALL;

    /// Debounce time between shock/vibrate requests (seconds)
    private float debounceTime = 0.1f;
    /// Whether to accumulate duration for multiple requests within the debounce time
    private boolean accumulateDuration = true;
    /// Whether to accumulate intensity for multiple requests within the debounce time
    private boolean accumulateIntensity = false;
    /// Whether to queue different-size shocks/vibrations separately
    private boolean queueDifferent = true;

    /// The type of PiShock API to use
    private @NonNull String apiType = DefaultShockBackends.PISHOCK_WEBSOCKET;

    /// Identifier for on-site logs
    private @NonNull String logIdentifier = "PiShock-Zap (Minecraft)";
    /// PiShock account username
    private @NonNull String username = "";
    /// PiShock account API key
    private @NonNull String apiKey = "";

    public void setLogIdentifier(String string) {
        logIdentifier = string.isBlank() ? "PiShock-Zap (Minecraft)" : string.trim();
    }

    /// PiShock device serial port
    private @NonNull String serialPort = "/dev/ttyACM0";
    /// PiShock device IDs (for serial API)
    private @NonNull List<Integer> deviceIds = List.of();

    /// Custom Webhook URL
    private @NonNull String customWebhookUrl = "";

    /// OpenShock API token
    private @NonNull String openShockApiToken = "";
    /// OpenShock shocker IDs
    private @NonNull List<String> openShockShockerIds = List.of();

    /// PiShock (WebSocket backend) user ID
    private int psUserId = -1;
    /// PiShock (WebSocket backend) hub/shocker mapping
    private Int2ObjectMap<IntList> psHubShockers = new Int2ObjectArrayMap<>();

    /// OpenShock devices for serial use
    private @NonNull List<ShockDevice> openShockSerialDevices = List.of();

    private boolean fieldIsListOfInteger(@NonNull Field field) {
        return field.getName().equals("deviceIds");
    }

    private boolean fieldIsMapOfIntToListOfInt(@NonNull Field field) {
        return field.getName().equals("psHubShockers");
    }

    private boolean fieldIsListOfOpenShockDevice(@NonNull Field field) {
        return field.getName().equals("openShockSerialDevices");
    }

    @SuppressWarnings("unchecked")
    private void setSingleConfigField(@NonNull Field field, @NonNull Method setter, @NonNull Object value) {
        try {
            Class<?> type = field.getType();
            if (type.isAssignableFrom(ShockDistribution.class)) {
                value = ShockDistribution.valueOf((String) value);
            } else if ((type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) && value instanceof Number) {
                value = ((Number) value).intValue();
            } else if ((type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) && value instanceof Number) {
                value = ((Number) value).floatValue();
            } else if (type.isAssignableFrom(List.class) && fieldIsListOfInteger(field)) {
                // Unchecked cast, but quickly checked when doing `Number::intValue`
                value = ((List<Number>) value).stream().map(Number::intValue).collect(Collectors.toList());
            } else if (type.isAssignableFrom(List.class) && fieldIsListOfOpenShockDevice(field)) {
                value = Gsons.gson.fromJson(Gsons.gson.toJson(value), new TypeToken<List<ShockDevice>>() {
                }.getType());
            } else if (type.isAssignableFrom(Int2ObjectArrayMap.class) && fieldIsMapOfIntToListOfInt(field) && value instanceof Map<?, ?> m) {
                value = mapToInt2IntListMap(m);
            }
            setter.invoke(this, value);
        } catch (IllegalAccessException | InvocationTargetException | NumberFormatException e) {
            log.error("Failed to set config field {} with value {}", field.getName(), value, e);
        } catch (IllegalArgumentException | ClassCastException e) {
            log.error("Config value {} is not of type {} (got {})", field.getName(), field.getType().getName(), value.getClass().getName());
        }
    }

    private static @NonNull Int2ObjectArrayMap<Object> mapToInt2IntListMap(Map<?, ?> m) {
        var newValue = new Int2ObjectArrayMap<>(m.size());
        for (var entry : m.entrySet()) {
            // JSON keys are always strings
            var mapKey = (String) entry.getKey();
            var mapValue = (List<?>) entry.getValue();
            var mapKeyInt = Integer.parseInt(mapKey);
            var mapValueIntList = new IntArrayList(mapValue.size());
            for (var integer : mapValue) {
                mapValueIntList.add(((Number) integer).intValue());
            }
            newValue.put(mapKeyInt, mapValueIntList);
        }
        return newValue;
    }

    private @NonNull Map<String, Object> performConfigMigrations(@NonNull Map<String, Object> config) {
        int configVersion;
        if (!(config.get(CONFIG_VERSION_KEY) instanceof Number configVersionNumber)) {
            configVersion = 0;
        } else {
            configVersion = configVersionNumber.intValue();
        }

        if (configVersion == CONFIG_VERSION) {
            return config;
        }

        config = new HashMap<>(config);

        if (configVersion < 1) {
            // Migrate from integer damage equivalent to float damage equivalent
            if (config.get("vibrationThreshold") instanceof Number vibrationThresholdInt) {
                config.put("vibrationThreshold", vibrationThresholdInt.floatValue() * 0.05f);
            }
            if (config.get("maxDamage") instanceof Number maxDamageInt) {
                config.put("maxDamage", maxDamageInt.floatValue() * 0.05f);
            }
        }

        if (configVersion < 2) {
            // Migrate from localEnabled to API type enum
            if (config.get("localEnabled") instanceof Boolean localEnabled) {
                config.put("apiType", localEnabled ? "SERIAL" : "WEB_V1");
            }
        }

        if (configVersion < 3) {
            // Migrate from API type enum to registry
            if (config.get("apiType") instanceof String s) {
                config.put("apiType", switch (s) {
                    case "SERIAL" -> DefaultShockBackends.PISHOCK_SERIAL;
                    case "WEBHOOK" -> DefaultShockBackends.WEBHOOK;
                    case "OPENSHOCK" -> DefaultShockBackends.OPENSHOCK_WEB;
                    default -> DefaultShockBackends.PISHOCK_WEB_V1;
                });
            }
        }

        config.put(CONFIG_VERSION_KEY, CONFIG_VERSION);

        return config;
    }

    private static String fromSetterName(String setterName) {
        if (setterName.length() < 4) return "";
        return Character.toLowerCase(setterName.charAt(3)) +
            setterName.substring(4);
    }

    @ApiStatus.Internal
    public void setFromConfig(@NonNull Map<String, Object> config) {
        config = performConfigMigrations(config);

        var setMethods = new HashMap<String, Method>();
        for (Method method : getClass().getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge() || method.getParameterCount() != 1 || Modifier.isStatic(method.getModifiers()))
                continue;

            String name = fromSetterName(method.getName());
            if (name.isEmpty()) continue;

            setMethods.put(name, method);
        }

        for (Field field : getClass().getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.isSynthetic() || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }

            String fieldName = field.getName();
            Method setter = setMethods.get(fieldName);
            if (setter == null) {
                log.warn("Missing setter for config field {}", fieldName);
                continue;
            }
            Object value = config.get(fieldName);
            if (value != null) {
                setSingleConfigField(field, setter, value);
            }
        }
    }

    @ApiStatus.Internal
    public void copyToConfig(@NonNull Map<String, Object> config) {
        for (Field field : getClass().getDeclaredFields()) {
            try {
                int modifiers = field.getModifiers();
                if (field.isSynthetic() || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }

                Object value = field.get(this);
                if (value instanceof Enum<?> enumVal) {
                    value = enumVal.name();
                }
                config.put(field.getName(), value);
            } catch (IllegalAccessException e) {
                log.warn("Failed to access config field {}", field.getName(), e);
            }
        }

        config.put(CONFIG_VERSION_KEY, CONFIG_VERSION);
    }
}
