package moe.score.pishockzap.backend.impls;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.backend.BulkHttpRequestShockBackend;
import moe.score.pishockzap.backend.ConnectionTestResult;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SimpleHttpRequestShockBackend;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.config.internal.WebHookApiConfig;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static moe.score.pishockzap.util.Gsons.gson;

public class WebHookBackend extends BulkHttpRequestShockBackend<Map<String, Object>> {
    public WebHookBackend(PishockZapConfig config, Executor executor) {
        super(config, executor);
    }

    @Override
    protected Map<String, Object> generateDataForOperation(ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        return makePostData(distribution, op, intensity, duration);
    }

    private static @NonNull Map<String, Object> makePostData(ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        return Map.of(
            "operation", op.name(),
            "intensity", intensity,
            "duration", duration,
            "distribution", distribution.name());
    }

    @Override
    protected @NonNull URI getUri(Map<String, Object> data) {
        return URI.create(config.getCustomWebhookUrl());
    }

    @Override
    protected @NonNull Map<String, String> getHeaders(Map<String, Object> data) {
        return Map.of();
    }

    @Override
    protected @Nullable String getPostBody(Map<String, Object> data) {
        return gson.toJson(data);
    }

    @Override
    protected void onResponse(Map<String, Object> data, @NonNull String response) {
        logger.info("Custom Webhook call successful, request: " + gson.toJson(data) + "\nresponse: " + response);
    }

    @Override
    public boolean isConfigured() {
        try {
            getUri(Map.of());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @RequiredArgsConstructor
    public static class ConnectionTest extends SimpleHttpRequestShockBackend.ConnectionTest {
        private final WebHookApiConfig config;

        @Override
        public CompletableFuture<ConnectionTestResult> testConnection() {
            return testConnection(() ->
                gson.toJson(makePostData(ShockDistribution.RANDOM, OpType.VIBRATE, 0, 0)));
        }

        @Override
        public CompletableFuture<ConnectionTestResult> testVibration() {
            return testConnection(() ->
                gson.toJson(makePostData(ShockDistribution.ALL, OpType.VIBRATE, config.getVibrationIntensityMax(), config.getDuration())));
        }

        private CompletableFuture<ConnectionTestResult> testConnection(Supplier<String> postBody) {
            var uriStr = config.getCustomWebhookUrl();
            if (uriStr.isBlank())
                return CompletableFuture.completedFuture(ConnectionTestResult.NOT_CONFIGURED);

            URI uri;
            try {
                uri = new URI(uriStr);
            } catch (URISyntaxException e) {
                return CompletableFuture.completedFuture(ConnectionTestResult.NOT_CONFIGURED);
            }

            return makeRequest(uri, Map.of(), postBody.get())
                .thenApply(resp -> {
                    var statusCode = resp.statusCode();
                    System.err.println("Connection test response code: " + statusCode + ", body: " + resp.body());
                    if (statusCode >= 200 && statusCode < 300) {
                        return ConnectionTestResult.SUCCESS;
                    } else {
                        return ConnectionTestResult.UNKNOWN_ERROR;
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Connection test failed; exception thrown");
                    e.printStackTrace();
                    if (e instanceof IllegalArgumentException && e.getMessage().contains("invalid URI scheme")) {
                        return ConnectionTestResult.NOT_CONFIGURED;
                    }
                    return ConnectionTestResult.UNKNOWN_ERROR;
                });
        }
    }
}
