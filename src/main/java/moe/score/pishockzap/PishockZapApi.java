package moe.score.pishockzap;

import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackend;
import moe.score.pishockzap.frontend.ShockFrontend;

/**
 * Main API surface for consumption from other mods.
 */
@SuppressWarnings({"DataFlowIssue", "unused"})
public class PishockZapApi {
    private PishockZapApi() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the maximum intensity you may use for a given operation.
     * Requests which exceed this intensity will be ignored.
     *
     * @param op the operation this applies to
     * @return the highest allowed intensity value
     */
    public static int getMaxIntensity(OpType op) {
        var config = PishockZapMod.getInstance().getConfig();
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
        var mod = PishockZapMod.getInstance();
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
     * Gets the {@link ShockFrontend} through which shocks from damage are queued.
     */
    public static ShockFrontend getFrontend() {
        return PishockZapMod.getInstance().getZapController();
    }

    /**
     * Gets the {@link ShockBackend} through which more fine-grained shock requests can be performed.
     * You must stay within the limits as specified in {@link PishockZapApi#getMaxIntensity(OpType)}
     * and {@link PishockZapApi#getMaxDuration(OpType)}, otherwise your requests will be ignored.
     */
    public static ShockBackend getBackend() {
        return PishockZapMod.getInstance().getZapController().getBackend();
    }

    /**
     * Zap the user. Quick convenience method to send a shock within acceptable parameters, or a vibration if
     * a shock is not possible.
     *
     * @param intensityFraction the intensity from 0 to 1 (will be scaled to something within the user's limits)
     */
    public static void zap(float intensityFraction) {
        var config = PishockZapMod.getInstance().getConfig();
        float vibrationThreshold = config.getVibrationThreshold();
        float minThreshold = config.getMinDamage();
        if (vibrationThreshold < 1 && vibrationThreshold > minThreshold) minThreshold = vibrationThreshold;
        float scaled = (intensityFraction + minThreshold) / (config.getMaxDamage() - config.getMinDamage());
        getFrontend().queueShock(
            config.getShockDistribution(),
            false,
            Math.max(Math.min(scaled, 1), 0));
    }
}
