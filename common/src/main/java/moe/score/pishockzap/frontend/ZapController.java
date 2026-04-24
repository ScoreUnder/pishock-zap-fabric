package moe.score.pishockzap.frontend;

import lombok.Getter;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackend;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.ApiStatus;

import java.util.logging.Logger;

/**
 * Takes in a stream of incoming damage events and users the ShockQueue to
 * process them according to the limits and backoff settings in the
 * configuration, then sends them to the PiShock API.
 */
@ApiStatus.Internal
public class ZapController implements ShockFrontend {
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    @Getter
    @NonNull
    private volatile ShockBackend backend;
    private final Thread thread = new Thread(this::run);
    private final @NonNull PishockZapConfig config;
    private final @NonNull ShockQueue shockQueue;

    public ZapController(@NonNull ShockBackend backend, @NonNull PishockZapConfig config) {
        this.backend = backend;
        this.config = config;
        this.shockQueue = new ShockQueue(config);
    }

    public void start() {
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void stop() {
        this.thread.interrupt();
    }

    public void setBackend(@NonNull ShockBackend api) {
        this.backend.close();
        this.backend = api;
    }

    private void run() {
        while (true) {
            try {
                var shockData = shockQueue.takeAndMergeShocks();
                logger.info("Performing shock: " + shockData);
                var ok = backend.performOp(shockData.distribution(), shockData.type(), shockData.intensity(), shockData.duration());

                if (ok) {
                    // Waiting for shock to complete and then waiting for debounce time, so not a busy-wait per se
                    //noinspection BusyWait
                    Thread.sleep((long) ((shockData.duration() + config.getDebounceTime()) * 1000.0f));
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void queueShock(@NonNull ShockDistribution distribution, boolean isDeath, float damageEquivalent) {
        logger.info("Queueing shock: " + distribution + ", " + isDeath + ", " + damageEquivalent);
        shockQueue.queueShock(distribution, isDeath, damageEquivalent);
    }

    @Override
    public void queueRawShock(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        logger.info("Queueing raw shock: " + distribution + ", " + op + ", " + intensity + "%, " + duration + "s");
        shockQueue.queueRawShock(distribution, op, intensity, duration);
    }

    @Override
    public void queueShockForDamage(float hp, float maxHealth, float damage) {
        if (hp == maxHealth || maxHealth <= 0) {
            // Player is at full HP, can this really be called damage?
            // (Just in case other mods play with max health, it's not fair to zap the player for that)
            // Note: this return must be after updating player HP in the watcher, otherwise the watcher will
            // report incorrect damage the next time the player takes damage.
            return;
        }

        if (damage > 0) {
            boolean deathZap = hp == 0;
            ShockDistribution distribution = deathZap && config.isShockOnDeath() ? config.getShockDistributionDeath() : config.getShockDistribution();
            float damageEquivalent = config.isShockOnHealth() ? maxHealth - hp : damage;
            damageEquivalent /= maxHealth;
            if (damageEquivalent > 1.0f) {
                logger.warning("Damage equivalent " + damageEquivalent + " exceeds 100% damage, capping");
                damageEquivalent = 1.0f;
            }
            logger.info("Death? " + deathZap + ", damage: " + damage + ", hp: " + hp + ", damage equivalent: " + damageEquivalent);
            queueShock(distribution, deathZap, damageEquivalent);
        }
    }
}
