package moe.score.pishockzap.compat.mixin.pool;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.BooleanSupplier;

@ApiStatus.Internal
public interface HasRequirement {
    boolean pishockzap$isRequirementSatisfied();

    void pishockzap$setRequirement(BooleanSupplier requirement);
}
