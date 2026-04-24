package moe.score.pishockzap;

import lombok.experimental.UtilityClass;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackend;
import moe.score.pishockzap.frontend.ShockFrontend;
import org.jetbrains.annotations.ApiStatus;

/**
 * Main API surface for consumption from other mods.
 */
@SuppressWarnings("unused")
@ApiStatus.AvailableSince("2.0.0")
@UtilityClass
public class PishockZapApi {
    /**
     * Gets the maximum intensity you may use for a given operation.
     * Requests which exceed this intensity will be ignored.
     *
     * @param op the operation this applies to
     * @return the highest allowed intensity value
     */
    public static int getMaxIntensity(OpType op) {
        var config = mod().getConfig();
        return switch (op) {
            case SHOCK -> config.isShockOnDeath()
                ? Math.max(config.getShockIntensityMax(), config.getShockIntensityDeath())
                : config.getShockIntensityMax();
            case VIBRATE -> config.isShockOnDeath() && config.isVibrationOnly()
                ? Math.max(config.getVibrationIntensityMax(), config.getShockIntensityDeath())
                : config.getVibrationIntensityMax();
            default -> 0;
        };
    }

    /**
     * Gets the maximum duration (in seconds) you may use for a given operation.
     * Requests which exceed this duration will be ignored.
     *
     * @param op the operation this applies to
     * @return the highest allowed duration
     */
    public static float getMaxDuration(OpType op) {
        var mod = mod();
        var config = mod.getConfig();
        return Math.min(
            mod.getZapController().getBackend().getMaxDuration(),
            config.isShockOnDeath()
                ? Math.max(config.getMaxDuration(), config.getShockDurationDeath())
                : config.getMaxDuration());
    }

    /**
     * Returns whether the mod is enabled or not.
     */
    public static boolean isEnabled() {
        var mod = PishockZapMod.getInstance();
        if (mod == null) return false;
        return mod.getConfig().isEnabled();
    }

    /**
     * Returns whether the mod is initialised or not.
     * If this method returns {@code false}, it is not safe to call any other methods in this API except for
     * {@link #isEnabled()}, as the mod instance may not be available yet.
     * Once this returns {@code true}, it will never return {@code false} again, so you can safely call other methods
     * without re-checking.
     */
    @ApiStatus.AvailableSince("2.1.0")
    public static boolean isInitialised() {
        return PishockZapMod.getInstance() != null;
    }

    /**
     * Gets the {@link ShockFrontend} through which shocks from damage are queued.
     * You may also queue your own raw operations here, which are not subject to the usual shock merging rules that
     * the player may have configured for shocks from damage.
     */
    public static ShockFrontend getFrontend() {
        return mod().getZapController();
    }

    /**
     * Gets the {@link ShockBackend} through which more fine-grained shock requests can be performed.
     * You must stay within the limits as specified in {@link PishockZapApi#getMaxIntensity(OpType)}
     * and {@link PishockZapApi#getMaxDuration(OpType)}, otherwise your requests will be ignored.
     * <p>
     * You should prefer using {@link #getFrontend()} for most use cases, as the backend does not manage shock timing,
     * and you may end up with multiple consumers fighting for control over the same device.
     */
    public static ShockBackend getBackend() {
        return mod().getZapController().getBackend();
    }

    /**
     * Zap the user. Quick convenience method to send a shock within acceptable parameters, or a vibration if
     * a shock is not possible.
     *
     * @param intensityFraction the intensity from 0 to 1 (will be scaled to something within the user's limits)
     */
    public static void zap(float intensityFraction) {
        var config = mod().getConfig();
        float vibrationThreshold = config.getVibrationThreshold();
        float minThreshold = config.getMinDamage();
        if (vibrationThreshold < 1 && vibrationThreshold > minThreshold) minThreshold = vibrationThreshold;
        float scaled = (intensityFraction + minThreshold) / (config.getMaxDamage() - config.getMinDamage());
        getFrontend().queueShock(
            config.getShockDistribution(),
            false,
            Math.max(Math.min(scaled, 1), 0));
    }

    private static PishockZapMod mod() {
        var mod = PishockZapMod.getInstance();
        if (mod == null) {
            throw new IllegalStateException("PishockZapMod instance is not initialized yet");
        }
        return mod;
    }
}
