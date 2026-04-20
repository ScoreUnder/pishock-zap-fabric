package moe.score.pishockzap;

import lombok.NonNull;
import net.minecraft.entity.LivingEntity;

import java.lang.ref.WeakReference;

public class PlayerHpWatcher {
    private static final WeakReference<LivingEntity> NULL_WEAK_REFERENCE = new WeakReference<>(null);
    private @NonNull WeakReference<LivingEntity> lastPlayer = NULL_WEAK_REFERENCE;
    private float lastPlayerHp = -1;
    private int ignore = 0;

    public float updatePlayerHpAndGetDamage(@NonNull LivingEntity player, float hp) {
        float damage = calculateDamage(player, hp);
        lastPlayerHp = hp;
        return damage;
    }

    public void updatePlayerHpBypassIgnore(float hp) {
        lastPlayerHp = hp;
    }

    private float calculateDamage(@NonNull LivingEntity player, float hp) {
        if (player != lastPlayer.get()) {
            lastPlayer = new WeakReference<>(player);
            // Ignore the first update after the player is changed
            // because loading into a world will trigger 2 updates
            ignore = 1;
            return 0;
        }

        if (ignore != 0) {
            ignore--;
            return 0;
        }

        return lastPlayerHp - hp;
    }

    public void resetPlayer() {
        lastPlayer = NULL_WEAK_REFERENCE;
        lastPlayerHp = -1;
    }
}
