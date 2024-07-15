package moe.score.pishockzap.shockcalculation;

import lombok.Getter;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.PiShockApi;

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
    private volatile PiShockApi api;
    private final Thread thread;
    private final PishockZapConfig config;
    private final ShockQueue shockQueue;

    public ZapController(@NonNull PiShockApi api, @NonNull PishockZapConfig config) {
        this.api = api;
        this.thread = new Thread(this::run);
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

    public void setApi(@NonNull PiShockApi api) {
        this.api.close();
        this.api = api;
    }

    private void run() {
        while (true) {
            try {
                var shockData = shockQueue.takeAndMergeShocks();
                logger.info("Performing shock: " + shockData);
                api.performOp(shockData.distribution(), shockData.type(), shockData.intensity(), shockData.duration());

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
