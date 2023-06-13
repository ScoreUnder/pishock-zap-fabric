package moe.score.pishockzap;

import net.minecraft.entity.LivingEntity;

public class PlayerHpWatcher {
    private int lastPlayer = -1;
    private int lastPlayerHp = -1;

    public int updatePlayerHpAndGetDamage(LivingEntity player, int hp) {
        int playerId = System.identityHashCode(player);
        if (playerId != lastPlayer) {
            lastPlayer = playerId;
            lastPlayerHp = hp;
            return 0;
        }

        int damage = lastPlayerHp - hp;
        lastPlayerHp = hp;
        return damage;
    }
}
