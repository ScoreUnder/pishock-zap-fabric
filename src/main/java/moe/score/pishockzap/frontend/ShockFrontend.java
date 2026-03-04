package moe.score.pishockzap.frontend;

import lombok.NonNull;
import moe.score.pishockzap.backend.OpType;
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
     * Queue the given operation on the user's shock device(s).
     * Note that this should respect the user's limits (see
     * {@link moe.score.pishockzap.PishockZapApi#getMaxDuration(OpType)} and
     * {@link moe.score.pishockzap.PishockZapApi#getMaxIntensity(OpType)}) otherwise it will be ignored.
     *
     * @param distribution which devices to activate
     * @param op           which operation to perform
     * @param intensity    the intensity (0-100). note that 99 and 100 are the same on PiShock.
     *                     0 will not operate the device, but (depending on implementation) may stop an ongoing operation.
     * @param duration     the desired length of the operation, in seconds
     */
    void queueRawShock(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration);

    /**
     * Calculate and queue a shock/vibration based on the damage taken and HP of the player.
     *
     * @param hp        the remaining HP of the player after damage is delivered
     * @param maxHealth the max HP of the player
     * @param damage    the damage that was delivered
     */
    void queueShockForDamage(float hp, float maxHealth, float damage);
}
