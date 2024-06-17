package moe.score.pishockzap.pishockapi;

import moe.score.pishockzap.config.ShockDistribution;

public interface PiShockApi {
    void performOp(ShockDistribution distribution, OpType op, int intensity, float duration);
    default void close() {
    }
}
