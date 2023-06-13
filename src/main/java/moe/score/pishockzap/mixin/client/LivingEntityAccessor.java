package moe.score.pishockzap.mixin.client;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("HEALTH")
    static net.minecraft.entity.data.TrackedData<Float> getHealth() {
        throw new AssertionError();
    }
}
