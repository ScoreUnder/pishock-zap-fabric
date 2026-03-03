package moe.score.pishockzap.frontend;

import lombok.Getter;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.backend.ShockBackend;

import java.util.logging.Logger;

/**
 * Takes in a stream of incoming damage events and users the ShockQueue to
 * process them according to the limits and backoff settings in the
 * configuration, then sends them to the PiShock API.
 */
public class ZapController {
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
                backend.performOp(shockData.distribution(), shockData.type(), shockData.intensity(), shockData.duration());

                // Waiting for shock to complete and then waiting for debounce time, so not a busy-wait per se
                //noinspection BusyWait
                Thread.sleep((long) ((shockData.duration() + config.getDebounceTime()) * 1000.0f));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void queueShock(@NonNull ShockDistribution distribution, boolean isDeath, float damageEquivalent) {
        logger.info("Queueing shock: " + distribution + ", " + isDeath + ", " + damageEquivalent);
        shockQueue.queueShock(distribution, isDeath, damageEquivalent);
    }
}
