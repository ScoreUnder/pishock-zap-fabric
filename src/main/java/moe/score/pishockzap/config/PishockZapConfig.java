package moe.score.pishockzap.config;

import lombok.Data;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class PishockZapConfig {
    static final String CONFIG_VERSION_KEY = "CONFIG_VERSION_DO_NOT_EDIT";
    static final int CONFIG_VERSION = 2;

    /// Whether the mod is enabled at all
    private boolean enabled = false;
    /// Whether shocks should be sent as vibrations instead
    private boolean vibrationOnly = false;
    /// Whether to shock/vibrate when the player dies
    private boolean shockOnDeath = true;
    /// Whether to shock/vibrate based on the player's health rather than damage
    private boolean shockOnHealth = false;

    /// The duration per shock/vibration
    private float duration = 1.0f;
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
    private ShockDistribution shockDistribution = ShockDistribution.ROUND_ROBIN;
    /// The distribution of shocks when the player dies
    private ShockDistribution shockDistributionDeath = ShockDistribution.ALL;

    /// Debounce time between shock/vibrate requests (seconds)
    private float debounceTime = 1.0f;
    /// Whether to accumulate duration for multiple requests within the debounce time
    private boolean accumulateDuration = true;
    /// Whether to accumulate intensity for multiple requests within the debounce time
    private boolean accumulateIntensity = false;
    /// Whether to queue different-size shocks/vibrations separately
    private boolean queueDifferent = true;

    /// The type of PiShock API to use
    private PiShockApiType apiType = PiShockApiType.WEB_V1;

    /// Identifier for on-site logs
    private String logIdentifier = "PiShock-Zap (Minecraft)";
    /// PiShock account username
    private String username = "";
    /// PiShock account API key
    private String apiKey = "";
    /// PiShock device share codes
    private List<String> shareCodes = List.of("BADC0DE0000");

    /// PiShock device serial port
    private String serialPort = "/dev/ttyACM0";
    /// PiShock device IDs (for serial API)
    private List<Integer> deviceIds = List.of(12345);

    /// Custom Webhook URL
    private String customWebhookUrl = "";

    private boolean fieldIsListOfInteger(Field field) {
        return field.getName().equals("deviceIds");
    }

    private void setSingleConfigField(Field field, Object value) {
        try {
            Class<?> type = field.getType();
            if (type.isAssignableFrom(ShockDistribution.class)) {
                value = ShockDistribution.valueOf((String) value);
            } else if (type.isAssignableFrom(PiShockApiType.class)) {
                value = PiShockApiType.valueOf((String) value);
            } else if ((type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) && value instanceof Number) {
                value = ((Number) value).intValue();
            } else if ((type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) && value instanceof Number) {
                value = ((Number) value).floatValue();
            } else if (type.isAssignableFrom(List.class) && fieldIsListOfInteger(field)) {
                // noinspection unchecked -- gets checked pretty damn quickly
                value = ((List<Number>) value).stream().map(Number::intValue).collect(Collectors.toList());
            }
            field.set(this, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException | ClassCastException e) {
            System.err.printf("Config value %s is not of type %s (got %s)%n", field.getName(), field.getType().getName(), value.getClass().getName());
        }
    }

    private Map<String, Object> performConfigMigrations(Map<String, Object> config) {
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
                config.put("apiType", localEnabled ? PiShockApiType.SERIAL.name() : PiShockApiType.WEB_V1.name());
            }
        }

        config.put(CONFIG_VERSION_KEY, CONFIG_VERSION);

        return config;
    }

    public void setFromConfig(Map<String, Object> config) {
        config = performConfigMigrations(config);

        for (Field field : getClass().getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (field.isSynthetic() || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }

            Object value = config.get(field.getName());
            if (value != null) {
                setSingleConfigField(field, value);
            }
        }
    }

    public void copyToConfig(Map<String, Object> config) {
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
                e.printStackTrace();
            }
        }

        config.put(CONFIG_VERSION_KEY, CONFIG_VERSION);
    }
}
