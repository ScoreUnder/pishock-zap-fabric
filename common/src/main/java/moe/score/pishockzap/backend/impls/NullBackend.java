package moe.score.pishockzap.backend.impls;

import lombok.NonNull;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackend;
import moe.score.pishockzap.config.ShockDistribution;

public class NullBackend implements ShockBackend {
    @Override
    public boolean performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        return false;
    }
}
