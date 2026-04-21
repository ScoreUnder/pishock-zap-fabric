package moe.score.pishockzap.mixin.client;

import moe.score.pishockzap.PishockZapMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(value = LivingEntity.class, priority = 2000)  // High priority because monitoring final outcome
public abstract class PlayerHpMixin {
    @Inject(at = @At("RETURN"), method = "setHealth")
    private void checkHpDamage(float health, CallbackInfo info) {
        var mcPlayer = Minecraft.getInstance().player;
        if (mcPlayer != (Object) this) {
            return;
        }

        PishockZapMod.getInstance().onPlayerHpChange(mcPlayer);
    }
}
