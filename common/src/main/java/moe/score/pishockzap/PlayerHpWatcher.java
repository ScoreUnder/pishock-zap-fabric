package moe.score.pishockzap;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.WeakReference;

@ApiStatus.Internal
public class PlayerHpWatcher<T> {
    private static final WeakReference<Object> NULL_WEAK_REFERENCE = new WeakReference<>(null);
    @SuppressWarnings("unchecked")
    private @NonNull WeakReference<T> lastPlayer = (WeakReference<T>) NULL_WEAK_REFERENCE;
    private float lastPlayerHp = -1;
    private int ignore = 0;

    public float updatePlayerHpAndGetDamage(@NonNull T player, float hp) {
        float damage = calculateDamage(player, hp);
        lastPlayerHp = hp;
        return damage;
    }

    public void updatePlayerHpBypassIgnore(float hp) {
        lastPlayerHp = hp;
    }

    private float calculateDamage(@NonNull T player, float hp) {
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

    @SuppressWarnings("unchecked")
    public void resetPlayer() {
        lastPlayer = (WeakReference<T>) NULL_WEAK_REFERENCE;
        lastPlayerHp = -1;
    }
}
