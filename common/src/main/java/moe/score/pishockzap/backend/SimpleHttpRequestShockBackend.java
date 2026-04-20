package moe.score.pishockzap.backend;

import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SimpleHttpRequestShockBackend<T, U> extends SafeShockBackend {
    protected final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    protected final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    protected final Executor executor;
    protected final HttpClient httpClient;

    public SimpleHttpRequestShockBackend(PishockZapConfig config, Executor executor) {
        super(config);
        this.executor = executor;
        this.httpClient = HttpClient.newBuilder().executor(executor).build();
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
        CompletableFuture.supplyAsync(
                        () -> {
                            var req = HttpRequest.newBuilder(getUri(data));
                            var postBody = getPostBody(data);
                            if (postBody != null) {
                                req.POST(HttpRequest.BodyPublishers.ofString(postBody, StandardCharsets.UTF_8));
                            }
                            for (var header : getHeaders(data).entrySet()) {
                                req.setHeader(header.getKey(), header.getValue());
                            }
                            return req.build();
                        }, executor)
                .thenComposeAsync(
                        req -> httpClient.sendAsync(req, BodyHandlers.ofString(StandardCharsets.UTF_8)),
                        executor)
                .thenAcceptAsync(resp -> onResponse(data, resp.body()), executor)
                .whenComplete((v, e) -> {
                    if (e != null) logger.log(Level.WARNING, "API call failed; exception thrown", e);
                });
    }

    protected abstract U generateDataForOperation(T device, @NonNull OpType op, int intensity, float duration);

    protected abstract @NonNull URI getUri(U data);

    protected abstract @NonNull Map<String, String> getHeaders(U data);

    protected abstract @Nullable String getPostBody(U data);

    protected abstract void onResponse(U data, @NonNull String response);
}
