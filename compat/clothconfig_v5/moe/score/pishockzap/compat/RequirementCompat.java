package moe.score.pishockzap.compat;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import moe.score.pishockzap.compat.mixin.pool.HasRequirement;

import java.util.function.BooleanSupplier;

public class RequirementCompat {
    public static void setRequirement(AbstractConfigListEntry<?> entry, BooleanSupplier requirement) {
        if (entry instanceof HasRequirement req) {
            req.pishockzap$setRequirement(requirement);
        }
    }
}
