package moe.score.pishockzap.compat.mixin.pool;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.BooleanSupplier;

@ApiStatus.Internal
public interface HasDisplayRequirement {
    boolean pishockzap$isDisplayed();

    void pishockzap$setDisplayRequirement(BooleanSupplier requirement);
}
