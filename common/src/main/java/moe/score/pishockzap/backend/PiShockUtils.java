package moe.score.pishockzap.backend;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.ApiStatus;

import java.util.Random;

@UtilityClass
@ApiStatus.Internal
@Slf4j(topic = Constants.NAME)
public class PiShockUtils {
    public static final int PISHOCK_MAX_DURATION = 15;
    public static final int PISHOCK_MAX_INTENSITY = 100;

    public static boolean shockParamsAreValid(int intensity, float duration, float intensityMax, float durationMax) {
        if (intensity == 0 || duration == 0.0f) return false;
        if (intensity < 0 || intensity > intensityMax) {
            log.warn("Shock operation intensity out of range: {}", intensity);
            return false;
        }
        if (duration < 0.0f || duration > durationMax || !Float.isFinite(duration)) {
            log.warn("Shock operation duration out of range: {}", duration);
            return false;
        }
        return true;
    }

    public static class ShockDistributor {
        private int roundRobinIndex = 0;
        private final Random random = new Random();

        /**
         * Pick from a list of shockers by distribution.
         *
         * @param distribution distribution to use
         * @param length       number of shockers to pick from
         * @return a boolean array of length {@code length} with {@code true} for each shocker to shock
         */
        public boolean @NonNull [] pickShockers(@NonNull ShockDistribution distribution, int length) {
            boolean[] shocks = new boolean[length];
            int randomIndex = random.nextInt(length);
            if (roundRobinIndex >= length) roundRobinIndex = 0;

            for (int i = 0; i < length; i++) {
                shocks[i] = switch (distribution) {
                    case ALL -> true;
                    case ROUND_ROBIN -> i == roundRobinIndex;
                    case RANDOM -> i == randomIndex;
                    case RANDOM_ALL -> random.nextBoolean();
                    case FIRST -> i == 0;
                    case LAST -> i == shocks.length - 1;
                };
            }

            roundRobinIndex++;

            if (distribution == ShockDistribution.RANDOM_ALL) {
                boolean hasShock = false;
                for (boolean shock : shocks) {
                    if (shock) {
                        hasShock = true;
                        break;
                    }
                }
                // If no shocks were selected, select a random one
                if (!hasShock) shocks[randomIndex] = true;
            }
            return shocks;
        }
    }
}
