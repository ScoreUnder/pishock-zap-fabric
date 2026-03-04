package moe.score.pishockzap.backend;

import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.util.HttpUtil;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public abstract class SimpleHttpRequestShockBackend<T, U> extends SafeShockBackend {
    protected final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    protected final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    protected final Executor executor;

    public SimpleHttpRequestShockBackend(PishockZapConfig config, Executor executor) {
        super(config);
        this.executor = executor;
    }

    @Override
    public void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        var devices = getDevices();
        if (devices.isEmpty()) {
            logger.warning("Cannot " + op + ": No devices available!");
            return;
        }

        boolean[] shocks = distributor.pickShockers(distribution, devices.size());
        for (int i = 0; i < shocks.length; i++) {
            if (!shocks[i]) continue;
            doApiCallOnThread(generateDataForOperation(devices.get(i), op, intensity, duration));
        }
    }

    protected abstract @NonNull List<T> getDevices();


    /**
     * Perform an API call on a separate thread.
     *
     * @param data data to send
     */
    protected void doApiCallOnThread(U data) {
        executor.execute(() -> {
            try {
                byte[] response = HttpUtil.makeRequestSync(getUrl(data), getPostBody(data), getHeaders(data));
                onResponse(data, response);
            } catch (Exception e) {
                logger.warning("API call failed; exception thrown");
                e.printStackTrace();
            }
        });
    }

    protected abstract U generateDataForOperation(T device, @NonNull OpType op, int intensity, float duration);

    protected abstract @NonNull URL getUrl(U data) throws MalformedURLException;

    protected abstract @NonNull Map<String, String> getHeaders(U data);

    protected abstract byte @Nullable [] getPostBody(U data);

    protected abstract void onResponse(U data, byte[] response);
}
