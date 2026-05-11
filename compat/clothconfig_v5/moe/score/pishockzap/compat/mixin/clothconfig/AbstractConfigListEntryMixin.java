package moe.score.pishockzap.compat.mixin.clothconfig;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import moe.score.pishockzap.compat.mixin.pool.HasRequirement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.BooleanSupplier;

@Mixin(AbstractConfigListEntry.class)
@Environment(EnvType.CLIENT)
public abstract class AbstractConfigListEntryMixin<T> extends AbstractConfigEntry<T> implements HasRequirement {
    @Unique
    private BooleanSupplier pishockzap$requirement = () -> true;

    @WrapMethod(method = "isEditable")
    private boolean pishockzap$wrapIsEditable(Operation<Boolean> original) {
        return original.call() && this.pishockzap$isRequirementSatisfied();
    }

    @Override
    public void pishockzap$setRequirement(BooleanSupplier requirement) {
        this.pishockzap$requirement = requirement;
    }

    @Override
    public boolean pishockzap$isRequirementSatisfied() {
        return this.pishockzap$requirement.getAsBoolean();
    }
}
