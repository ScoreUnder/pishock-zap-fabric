package moe.score.pishockzap.shockcalculation;

import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.OpType;

public record CalculatedShock(ShockDistribution distribution, OpType type, int intensity, float duration) {
}
