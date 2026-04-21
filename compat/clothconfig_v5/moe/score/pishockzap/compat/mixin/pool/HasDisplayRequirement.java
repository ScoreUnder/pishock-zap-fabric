package moe.score.pishockzap.compat.mixin.pool;

import java.util.function.BooleanSupplier;

public interface HasDisplayRequirement {
    boolean pishockzap$isDisplayed();

    void pishockzap$setDisplayRequirement(BooleanSupplier requirement);
}
