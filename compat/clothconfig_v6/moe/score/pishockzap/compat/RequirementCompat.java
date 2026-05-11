package moe.score.pishockzap.compat;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;

import java.util.function.BooleanSupplier;

public class RequirementCompat {
    @SuppressWarnings("UnstableApiUsage")
    public static void setRequirement(AbstractConfigListEntry<?> entry, BooleanSupplier requirement) {
        entry.setRequirement(requirement::getAsBoolean);
    }
}
