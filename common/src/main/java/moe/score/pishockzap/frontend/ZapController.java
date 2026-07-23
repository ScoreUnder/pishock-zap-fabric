package moe.score.pishockzap.frontend;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.ShockBackend;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.ApiStatus;

/**
 * Takes in a stream of incoming damage events and users the ShockQueue to
 * process them according to the limits and backoff settings in the
 * configuration, then sends them to the PiShock API.
 */
@ApiStatus.Internal
@Slf4j(topic = Constants.NAME)
public class ZapController implements ShockFrontend {
    @Getter
    @NonNull
    private volatile ShockBackend backend;
    private final Thread thread = new Thread(this::run, "ZapController");
    private final @NonNull PishockZapConfig config;
    private final @NonNull ShockQueue shockQueue;

    public ZapController(@NonNull ShockBackend backend, @NonNull PishockZapConfig config) {
        this.backend = backend;
        this.config = config;
        this.shockQueue = new ShockQueue(config);
        this.thread.setDaemon(true);
    }

    public void start() {
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

                if (shockData.type() == OpType.SHOCK && needWarnings()) {
                    log.info("Performing warning vibration for shock: {}", shockData);
                    var vibrateIntensity = Math.min(config.getVibrationIntensityMax(), Math.max(config.getVibrationIntensityMin(), shockData.intensity()));
                    performOpAndWait(shockData.distribution(), OpType.VIBRATE, vibrateIntensity, config.getWarningDuration(), config.getWarningDelay());
                }

                log.info("Performing shock: {}", shockData);
                performOpAndWait(shockData.distribution(), shockData.type(), shockData.intensity(), shockData.duration(), config.getDebounceTime());
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                log.error("Error in shock queue thread", e);
            }
        }
    }

    private boolean needWarnings() {
        return config.isUseWarningVibration() && !config.isVibrationOnly();
    }

    private void performOpAndWait(ShockDistribution distribution, OpType op, int intensity, float duration, float delay) throws InterruptedException {
        var ok = backend.performOp(distribution, op, intensity, duration);

        if (ok) {
            // Wait for shock to complete, and then wait for debounce time
            Thread.sleep((long) ((duration + delay) * 1000.0f));
        }
    }

    @Override
    public void queueShock(@NonNull ShockDistribution distribution, boolean isDeath, float damageEquivalent) {
        log.debug("Queueing shock: {}, {}, {}", distribution, isDeath, damageEquivalent);
        shockQueue.queueShock(distribution, isDeath, damageEquivalent);
    }

    @Override
    public void queueRawShock(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        log.debug("Queueing raw shock: {}, {}, {}%, {}s", distribution, op, intensity, duration);
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
                log.warn("Damage equivalent {} exceeds 100% damage, capping", damageEquivalent);
                damageEquivalent = 1.0f;
            }
            log.trace("Death? {}, damage: {}, hp: {}, damage equivalent: {}", deathZap, damage, hp, damageEquivalent);
            queueShock(distribution, deathZap, damageEquivalent);
        }
    }
}
