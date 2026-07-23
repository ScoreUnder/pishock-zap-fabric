package moe.score.pishockzap.config;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.DefaultShockBackends;
import moe.score.pishockzap.annotation.InternalMembers;
import moe.score.pishockzap.backend.model.openshock.ShockDevice;
import moe.score.pishockzap.config.internal.OpenShockWebApiConfig;
import moe.score.pishockzap.config.internal.PiShockWebSocketApiConfig;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Represents the full configuration of the Pishock-Zap mod. This is a live object (modified in-place), not a snapshot.
 * <p>
 * The internals of this class are not meant for API consumers and will change without warning.
 */
@Data
@ApiStatus.Experimental
@InternalMembers
@Slf4j(topic = Constants.NAME)
public class PishockZapConfig implements PiShockWebSocketApiConfig, OpenShockWebApiConfig {
    /// Whether the mod is enabled at all
    private boolean enabled = false;
    /// Whether shocks should be sent as vibrations instead
    private boolean vibrationOnly = false;
    /// Whether to shock on damage at all
    private boolean shockOnDamage = true;
    /// Whether to shock/vibrate when the player dies
    private boolean shockOnDeath = true;
    /// Whether to shock/vibrate based on the player's health rather than damage
    private boolean shockOnHealth = false;
    /// Whether to allow fractional half-heart damage
    private boolean fractionalDamage = false;
    /// Whether to send a warning vibration before shocking the player
    private boolean useWarningVibration = false;
    /// How long to send a warning vibration for before shocking the player
    private float warningDuration = 0.3f;
    /// How long to wait after sending the warning vibration before shocking the player
    private float warningDelay = 0.1f;

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
    /// Whether to allow other mods to bypass the intensity and duration limits
    private boolean allowBypassLimits = false;

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

    /// OpenShock API URI
    private @NonNull String openShockApiUri = "https://api.openshock.app";
    /// OpenShock API token
    private @NonNull String openShockApiToken = "";
    /// OpenShock shocker IDs
    private @NonNull List<@NonNull String> openShockShockerIds = List.of();

    /// PiShock (WebSocket backend) user ID
    private int psUserId = -1;
    /// PiShock (WebSocket backend) hub/shocker mapping
    private @NonNull Int2ObjectMap<@NonNull IntList> psHubShockers = new Int2ObjectArrayMap<>();

    /// OpenShock devices for serial use
    private @NonNull List<@NonNull ShockDevice> openShockSerialDevices = List.of();

    @ApiStatus.Internal
    public void updateFrom(@NonNull PishockZapConfig other) {
        for (Field field : PishockZapConfig.class.getDeclaredFields()) {
            try {
                int modifiers = field.getModifiers();
                if (field.isSynthetic() || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                    continue;
                }

                Object value = field.get(other);
                field.set(this, value);
            } catch (IllegalAccessException e) {
                log.warn("Failed to copy config field {}", field.getName(), e);
            }
        }
    }
}
