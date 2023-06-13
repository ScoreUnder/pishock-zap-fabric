package moe.score.pishockzap.config;

import lombok.Data;

import java.lang.reflect.Field;
import java.util.Map;

@Data
public class PishockZapConfig {
    /// Whether the mod is enabled at all
    private boolean enabled = false;
    /// Whether shocks should be sent as vibrations instead
    private boolean vibrationOnly = false;
    /// Whether to shock/vibrate when the player dies
    private boolean shockOnDeath = true;
    /// Whether to shock/vibrate based on the player's health rather than damage
    private boolean shockOnHealth = false;

    /// The duration per shock/vibration
    private int duration = 1;
    /// The maximum duration per shock/vibration, if it varies e.g. based on debouncing
    private int maxDuration = 10;
    /// The threshold to swap from vibration to shock
    private int vibrationThreshold = 0;
    /// The minimum intensity of a vibration
    private int vibrationIntensityMin = 20;
    /// The maximum intensity of a vibration
    private int vibrationIntensityMax = 100;
    /// The minimum intensity of a shock
    private int shockIntensityMin = 5;
    /// The maximum intensity of a shock
    private int shockIntensityMax = 60;
    /// The intensity of a shock when the player dies
    private int shockIntensityDeath = 80;
    /// The duration of a shock when the player dies
    private int shockDurationDeath = 10;
    /// The distribution of shocks when the player takes damage
    private ShockDistribution shockDistribution = ShockDistribution.ROUND_ROBIN;
    /// The distribution of shocks when the player dies
    private ShockDistribution shockDistributionDeath = ShockDistribution.ALL;

    /// Debounce time between shock/vibrate requests (seconds)
    private int debounceTime = 1;
    /// Whether to accumulate duration for multiple requests within the debounce time
    private boolean accumulateDuration = true;
    /// Whether to accumulate intensity for multiple requests within the debounce time
    private boolean accumulateIntensity = false;
    /// Whether to queue different-size shocks/vibrations separately
    private boolean queueDifferent = true;

    /// PiShock account username
    private String username = "";
    /// PiShock account API key
    private String apiKey = "";
    /// PiShock device share code
    private String shareCode = "";
    /// Identifier for on-site logs
    private String logIdentifier = "PiShock-Zap (Minecraft)";

    private void setSingleFromConfig(Map<String, Object> config, Field field, Object value) {
        try {
            field.set(this, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Config value " + field.getName() + " is not of type " + field.getType().getName());
        }
    }

    public void setFromConfig(Map<String, Object> config) {
        for (Field field : getClass().getDeclaredFields()) {
            Object value = config.get(field.getName());
            if (value != null) {
                setSingleFromConfig(config, field, value);
            }
        }
    }

    public void copyToConfig(Map<String, Object> config) {
        for (Field field : getClass().getDeclaredFields()) {
            try {
                config.put(field.getName(), field.get(this));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
