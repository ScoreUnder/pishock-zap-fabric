package moe.score.pishockzap.frontend;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.PiShockUtils;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.util.Either;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

@RequiredArgsConstructor
@ApiStatus.Internal
public class ShockQueue {
    private final Logger logger = Logger.getLogger(Constants.NAME);
    private final BlockingQueue<Either<QueuedShock, CalculatedShock>> queue = new LinkedBlockingQueue<>();
    @NonNull
    private final PishockZapConfig config;

    public @NonNull CalculatedShock takeAndMergeShocks() throws InterruptedException {
        var either = queue.take();
        if (either.isRight()) return either.right();

        QueuedShock shock = either.left();
        while ((either = queue.peek()) != null && either.isLeft()) {
            if (!mergeShock(shock, either.left())) break;
            queue.remove();  // queue should always have an item available to remove, because we just peeked earlier
        }

        return transformShock(shock);
    }

    // Used for test assertions, because they need to know if the queue has been emptied when expected
    // Under normal circumstances this is not useful due to thread safety issues
    boolean isEmpty() {
        return queue.isEmpty();
    }

    private boolean mergeShock(@NonNull QueuedShock shock, @NonNull QueuedShock nextShock) {
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
        float damageEquivalent;

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
            if (damageEquivalent < 0.0f || damageEquivalent > 1.0f) {
                return false;
            }
        } else {
            damageEquivalent = Math.max(shock.damageEquivalent, nextShock.damageEquivalent);
        }

        shock.duration = duration;
        shock.damageEquivalent = damageEquivalent;

        return true;
    }

    private @NonNull CalculatedShock transformShock(@NonNull QueuedShock shock) {
        boolean separateDeathShock = config.isShockOnDeath() && shock.isDeath;
        OpType type;
        int intensity;
        float duration;

        if (shock.damageEquivalent > 1.0f) {
            // Should never happen, but safety first
            logger.warning("Damage equivalent is greater than max damage, clamping to max damage");
            shock.damageEquivalent = 1.0f;
        }

        if (separateDeathShock) {
            type = config.isVibrationOnly() ? OpType.VIBRATE : OpType.SHOCK;
            intensity = config.getShockIntensityDeath();
            duration = config.getShockDurationDeath();

            // No sanity checks here, because we know the values are valid
            // and the user may want them to be out of the normal range
        } else {
            float vibrationThreshold = config.getVibrationThreshold();
            float minDamage = config.getMinDamage();
            if (config.isVibrationOnly()) vibrationThreshold = 1.0f;
            if (shock.damageEquivalent > vibrationThreshold) {
                type = OpType.SHOCK;
                // This bodge exists so that the minimum intensity corresponds to a half-heart of damage instead of 0 damage.
                // To accomplish this, we subtract an extra $minDamage of damage from the damage equivalent and range.
                intensity = transformIntensityIntoRange(
                    shock.damageEquivalent - vibrationThreshold - minDamage,
                    config.getMaxDamage() - vibrationThreshold - minDamage,
                    config.getShockIntensityMin(),
                    config.getShockIntensityMax());
            } else {
                type = OpType.VIBRATE;
                intensity = transformIntensityIntoRange(
                    shock.damageEquivalent - minDamage,
                    Math.min(vibrationThreshold, config.getMaxDamage()) - minDamage,
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

    private int sanityCheckIntensity(@NonNull OpType type, int intensity) {
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

    private int transformIntensityIntoRange(float damageEquivalent, float damageRange, int intensityMin, int intensityMax) {
        if (damageRange <= 0) return intensityMax;
        if (damageEquivalent <= 0) return intensityMin;
        if (damageEquivalent >= damageRange) return intensityMax;
        return Math.round((damageEquivalent / damageRange) * (intensityMax - intensityMin) + intensityMin);
    }

    public void queueShock(@NonNull ShockDistribution distribution, boolean isDeath, float damageEquivalent) {
        queue.add(Either.left(new QueuedShock(distribution, isDeath, damageEquivalent, config.getDuration())));
    }

    public void queueRawShock(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        queue.add(Either.right(new CalculatedShock(distribution, op, intensity, duration)));
    }

    @AllArgsConstructor
    private static final class QueuedShock {
        @NonNull
        private final ShockDistribution distribution;
        private final boolean isDeath;
        private float damageEquivalent;
        private float duration;
    }
}
