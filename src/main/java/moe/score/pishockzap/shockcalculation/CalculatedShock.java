package moe.score.pishockzap.shockcalculation;

import lombok.NonNull;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.OpType;

public record CalculatedShock(@NonNull ShockDistribution distribution, @NonNull OpType type, int intensity, float duration) {
}
