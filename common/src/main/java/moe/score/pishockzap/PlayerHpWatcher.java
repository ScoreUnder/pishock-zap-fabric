package moe.score.pishockzap;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.PhantomReference;

@ApiStatus.Internal
public class PlayerHpWatcher<T> {
    private static final PhantomReference<Object> NULL_PHANTOM_REFERENCE = new PhantomReference<>(null, null);
    // Player kept as a PhantomReference to prevent accidental retrieval of a stale Player object.
    @SuppressWarnings("unchecked")
    private @NonNull PhantomReference<T> lastPlayer = (PhantomReference<T>) NULL_PHANTOM_REFERENCE;
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
        if (!lastPlayer.refersTo(player)) {
            lastPlayer = new PhantomReference<>(player, null);
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
        lastPlayer = (PhantomReference<T>) NULL_PHANTOM_REFERENCE;
        lastPlayerHp = -1;
    }
}
