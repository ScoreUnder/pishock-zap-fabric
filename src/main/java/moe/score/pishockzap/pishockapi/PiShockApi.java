package moe.score.pishockzap.pishockapi;

import lombok.NonNull;
import moe.score.pishockzap.config.ShockDistribution;

public interface PiShockApi {
    void performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration);
    default void close() {
    }
}
