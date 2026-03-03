package moe.score.pishockzap.backend;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;

@RequiredArgsConstructor
public abstract class SafeShockBackend implements ShockBackend {
    protected final PishockZapConfig config;

    @Override
    public final void performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        if (!config.isEnabled()) return;
        if (config.isVibrationOnly()) op = OpType.VIBRATE;
        if (!isConfigured()) return;
        if (!areShockParamsValid(op, intensity, duration)) return;

        safePerformOp(distribution, op, intensity, duration);
    }

    protected boolean areShockParamsValid(OpType op, int intensity, float duration) {
        return PiShockUtils.shockParamsAreValid(intensity, duration);
    }

    protected abstract boolean isConfigured();

    protected abstract void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration);
}
