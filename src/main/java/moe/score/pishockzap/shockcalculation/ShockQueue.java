package moe.score.pishockzap.shockcalculation;

import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.OpType;
import moe.score.pishockzap.pishockapi.PiShockUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class ShockQueue {
    public static final int MAX_DAMAGE = 20;
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private final BlockingQueue<QueuedShock> queue = new LinkedBlockingQueue<>();
    private final PishockZapConfig config;

    public CalculatedShock takeAndMergeShocks() throws InterruptedException {
        QueuedShock shock = queue.take();
        QueuedShock nextShock;

        while ((nextShock = queue.peek()) != null) {
            if (!mergeShock(shock, nextShock)) break;
            queue.remove();  // should always have one because we just peeked earlier
        }

        return transformShock(shock);
    }

    // Used for test assertions, because they need to know if the queue has been emptied when expected
    // Under normal circumstances this is not useful due to thread safety issues
    boolean isEmpty() {
        return queue.isEmpty();
    }

    private boolean mergeShock(QueuedShock shock, QueuedShock nextShock) {
        if ((nextShock.isDeath || shock.isDeath) && config.isShockOnDeath()) {
            // Can't merge death shocks
            return false;
        }
        if (nextShock.distribution != shock.distribution) {
            // If the shock distributions are different, we can't merge them
            return false;
        }
        if (config.isQueueDifferent() && nextShock.damageEquivalent != shock.damageEquivalent) {
            // If the next shock intensity is different, and config says not to merge different shocks, we can't merge it
            return false;
        }

        float duration;
        int damageEquivalent;

        if (config.isAccumulateDuration()) {
            duration = shock.duration * shock.damageEquivalent + nextShock.duration * nextShock.damageEquivalent;
            duration /= Math.max(shock.damageEquivalent, nextShock.damageEquivalent);
            if (duration < 0.0f || duration > PiShockUtils.PISHOCK_MAX_DURATION) {
                return false;
            }
        } else {
            duration = Math.max(shock.duration, nextShock.duration);
        }

        if (config.isAccumulateIntensity() && !config.isQueueDifferent() && !config.isShockOnHealth()) {
            damageEquivalent = shock.damageEquivalent + nextShock.damageEquivalent;
            if (damageEquivalent < 0 || damageEquivalent > MAX_DAMAGE) {
                return false;
            }
        } else {
            damageEquivalent = Math.max(shock.damageEquivalent, nextShock.damageEquivalent);
        }

        shock.duration = duration;
        shock.damageEquivalent = damageEquivalent;

        return true;
    }

    private CalculatedShock transformShock(QueuedShock shock) {
        boolean separateDeathShock = config.isShockOnDeath() && shock.isDeath;
        OpType type;
        int intensity;
        float duration;

        if (shock.damageEquivalent > MAX_DAMAGE) {
            // Should never happen, but safety first
            logger.warning("Damage equivalent is greater than max damage, clamping to max damage");
            shock.damageEquivalent = MAX_DAMAGE;
        }

        if (separateDeathShock) {
            type = OpType.SHOCK;
            intensity = config.getShockIntensityDeath();
            duration = config.getShockDurationDeath();

            // No sanity checks here, because we know the values are valid
            // and the user may want them to be out of the normal range
        } else {
            int vibrationThreshold = config.getVibrationThreshold();
            if (config.isVibrationOnly()) vibrationThreshold = MAX_DAMAGE;
            if (shock.damageEquivalent > vibrationThreshold) {
                type = OpType.SHOCK;
                intensity = transformIntensityIntoRange(
                    shock.damageEquivalent - vibrationThreshold - 1,
                    config.getMaxDamage() - vibrationThreshold - 1,
                    config.getShockIntensityMin(),
                    config.getShockIntensityMax());
            } else {
                type = OpType.VIBRATE;
                intensity = transformIntensityIntoRange(
                    shock.damageEquivalent - 1,
                    Math.min(vibrationThreshold, config.getMaxDamage()) - 1,
                    config.getVibrationIntensityMin(),
                    config.getVibrationIntensityMax());
            }
            duration = shock.duration;

            intensity = sanityCheckIntensity(type, intensity);
            duration = sanityCheckDuration(duration);
        }

        return new CalculatedShock(shock.distribution, type, intensity, duration);
    }

    private float sanityCheckDuration(float duration) {
        if (duration < 0.0f || duration > PiShockUtils.PISHOCK_MAX_DURATION) {
            logger.warning("Duration out of range: " + duration);
            duration = Math.max(0.0f, Math.min(duration, PiShockUtils.PISHOCK_MAX_DURATION));
        }
        if (duration < config.getDuration() || duration > config.getMaxDuration()) {
            logger.warning("Duration out of configured range: " + duration);
            duration = Math.max(config.getDuration(), Math.min(duration, config.getMaxDuration()));
        }
        return duration;
    }

    private int sanityCheckIntensity(OpType type, int intensity) {
        if (intensity < 0 || intensity > PiShockUtils.PISHOCK_MAX_INTENSITY) {
            logger.warning("Intensity out of range: " + intensity);
            intensity = Math.max(0, Math.min(intensity, PiShockUtils.PISHOCK_MAX_INTENSITY));
        }
        if (type == OpType.SHOCK && (intensity < config.getShockIntensityMin() || intensity > config.getShockIntensityMax())) {
            logger.warning("Shock intensity out of range: " + intensity);
            intensity = Math.max(config.getShockIntensityMin(), Math.min(intensity, config.getShockIntensityMax()));
        }
        if (type == OpType.VIBRATE && (intensity < config.getVibrationIntensityMin() || intensity > config.getVibrationIntensityMax())) {
            logger.warning("Vibration intensity out of range: " + intensity);
            intensity = Math.max(config.getVibrationIntensityMin(), Math.min(intensity, config.getVibrationIntensityMax()));
        }
        return intensity;
    }

    private int transformIntensityIntoRange(int damageEquivalent, int damageRange, int intensityMin, int intensityMax) {
        if (damageRange <= 0) return intensityMax;
        if (damageEquivalent <= 0) return intensityMin;
        if (damageEquivalent >= damageRange) return intensityMax;
        return Math.round((damageEquivalent / (float) damageRange) * (intensityMax - intensityMin) + intensityMin);
    }

    public void queueShock(ShockDistribution distribution, boolean isDeath, int damageEquivalent) {
        queue.add(new QueuedShock(distribution, isDeath, damageEquivalent, config.getDuration()));
    }

    private static final class QueuedShock {
        private final ShockDistribution distribution;
        private final boolean isDeath;
        private int damageEquivalent;
        private float duration;

        public QueuedShock(ShockDistribution distribution, boolean isDeath, int damageEquivalent, float duration) {
            this.distribution = distribution;
            this.isDeath = isDeath;
            this.damageEquivalent = damageEquivalent;
            this.duration = duration;
        }
    }
}
