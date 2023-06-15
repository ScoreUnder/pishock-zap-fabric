package moe.score.pishockzap;

import net.minecraft.entity.LivingEntity;

import java.lang.ref.WeakReference;

public class PlayerHpWatcher {
    private static final WeakReference<LivingEntity> NULL_WEAK_REFERENCE = new WeakReference<>(null);
    private WeakReference<LivingEntity> lastPlayer = NULL_WEAK_REFERENCE;
    private int lastPlayerHp = -1;
    private int ignore = 0;

    public int updatePlayerHpAndGetDamage(LivingEntity player, int hp) {
        if (player != lastPlayer.get()) {
            lastPlayer = new WeakReference<>(player);
            lastPlayerHp = hp;
            // Ignore the first update after the player is changed
            // because loading into a world will trigger 2 updates
            ignore = 1;
            return 0;
        }

        if (ignore != 0) {
            ignore--;
            lastPlayerHp = hp;
            return 0;
        }

        int damage = lastPlayerHp - hp;
        lastPlayerHp = hp;
        return damage;
    }

    public void resetPlayer() {
        lastPlayer = NULL_WEAK_REFERENCE;
        lastPlayerHp = -1;
    }
}
