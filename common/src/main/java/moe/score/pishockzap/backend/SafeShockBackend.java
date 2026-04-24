package moe.score.pishockzap.backend;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.PishockZapApi;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.ApiStatus;

@RequiredArgsConstructor
public abstract class SafeShockBackend implements ShockBackend {
    @ApiStatus.Internal
    protected final PishockZapConfig config;

    @Override
    public final boolean performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        if (!config.isEnabled()) return false;
        if (config.isVibrationOnly()) op = OpType.VIBRATE;
        if (!isConfigured()) return false;
        if (intensity > PishockZapApi.getMaxIntensity(op) || duration > PishockZapApi.getMaxDuration(op)) return false;
        if (!areShockParamsValid(op, intensity, duration)) return false;

        safePerformOp(distribution, op, intensity, duration);
        return true;
    }

    protected boolean areShockParamsValid(OpType op, int intensity, float duration) {
        return PiShockUtils.shockParamsAreValid(intensity, duration, PiShockUtils.PISHOCK_MAX_INTENSITY, getMaxDuration());
    }

    protected abstract boolean isConfigured();

    protected abstract void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration);
}
