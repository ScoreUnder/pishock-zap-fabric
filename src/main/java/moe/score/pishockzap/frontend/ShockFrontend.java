package moe.score.pishockzap.frontend;

import lombok.NonNull;
import moe.score.pishockzap.config.ShockDistribution;

public interface ShockFrontend {
    /**
     * Calculate and queue a shock/vibration based on the user's settings and the relative amount of damage.
     * See {@link ShockFrontend#queueShockForDamage(float, float, float)} if you wish to take into account things like the
     * player's "shock by remaining health" preference, etc.
     *
     * @param distribution     the distribution of shock devices to use
     * @param isDeath          {@code true} if this should be treated as the player's death
     * @param damageEquivalent fraction of HP affected, from 0-1 inclusive.
     */
    void queueShock(@NonNull ShockDistribution distribution, boolean isDeath, float damageEquivalent);

    /**
     * Calculate and queue a shock/vibration based on the damage taken and HP of the player.
     *
     * @param hp        the remaining HP of the player after damage is delivered
     * @param maxHealth the max HP of the player
     * @param damage    the damage that was delivered
     */
    void queueShockForDamage(float hp, float maxHealth, float damage);
}
