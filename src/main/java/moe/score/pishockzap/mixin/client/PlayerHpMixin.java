package moe.score.pishockzap.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(LivingEntity.class)
public abstract class PlayerHpMixin {
	@Inject(at = @At("HEAD"), method = "onTrackedDataSet")
	private void checkHpDamage(TrackedData<?> data, CallbackInfo info) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		//noinspection ConstantValue
		if (player != (Object) this) {
			return;
		}

		if (data == LivingEntityAccessor.getHealth()) {
			System.out.println("Health changed! Now: " + player.getHealth());
		}
	}
}