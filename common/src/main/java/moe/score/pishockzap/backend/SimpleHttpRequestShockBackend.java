package moe.score.pishockzap.backend;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ApiStatus.Internal
@Slf4j(topic = Constants.NAME)
public abstract class SimpleHttpRequestShockBackend<T, U> extends SafeShockBackend {
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
            log.warn("Cannot {}: No devices available!", op);
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
                if (e != null) log.warn("API call failed; exception thrown", e);
            });
    }

    protected abstract U generateDataForOperation(T device, @NonNull OpType op, int intensity, float duration);

    protected abstract @NonNull URI getUri(U data);

    protected abstract @NonNull Map<String, String> getHeaders(U data);

    protected abstract @Nullable String getPostBody(U data);

    protected abstract void onResponse(U data, @NonNull String response);

    protected static abstract class ConnectionTest implements BackendConnectionTest {
        private final HttpClient httpClient = HttpClient.newBuilder().build();

        protected CompletableFuture<HttpResponse<String>> makeRequest(URI uri, Map<String, String> headers, @Nullable String postBody) {
            return CompletableFuture.supplyAsync(() -> {
                var req = HttpRequest.newBuilder(uri);
                if (postBody != null) {
                    req.POST(HttpRequest.BodyPublishers.ofString(postBody, StandardCharsets.UTF_8));
                }
                for (var header : headers.entrySet()) {
                    req.setHeader(header.getKey(), header.getValue());
                }
                return httpClient.sendAsync(req.build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
            }).thenCompose(f -> f);
        }
    }
}
