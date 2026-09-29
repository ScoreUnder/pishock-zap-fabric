package moe.score.pishockzap.util;

import org.jetbrains.annotations.ApiStatus;

// Doesn't exist on old fastutil
@ApiStatus.Internal
public record IntObjectPair<T>(int left, T right) {
}
