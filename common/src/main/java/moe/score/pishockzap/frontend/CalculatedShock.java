package moe.score.pishockzap.frontend;

import lombok.NonNull;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.config.ShockDistribution;

public record CalculatedShock(@NonNull ShockDistribution distribution, @NonNull OpType type, int intensity, float duration) {
}
