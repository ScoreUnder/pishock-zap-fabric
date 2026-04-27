package moe.score.pishockzap.util;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@FunctionalInterface
public interface FloatSupplier {
    float getAsFloat();
}
