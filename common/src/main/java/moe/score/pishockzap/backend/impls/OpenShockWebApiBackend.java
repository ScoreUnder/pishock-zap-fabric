package moe.score.pishockzap.backend.impls;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.backend.BulkHttpRequestShockBackend;
import moe.score.pishockzap.backend.ConnectionTestResult;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SimpleHttpRequestShockBackend;
import moe.score.pishockzap.backend.model.openshock.Control;
import moe.score.pishockzap.backend.model.openshock.ControlType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.config.internal.OpenShockWebApiConfig;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static moe.score.pishockzap.backend.client.OpenShockWebClient.getDefaultHeaders;
import static moe.score.pishockzap.util.Gsons.gson;

public class OpenShockWebApiBackend extends BulkHttpRequestShockBackend<List<Control>> {
    private static final URI API_URI = URI.create("https://api.openshock.app/2/shockers/control");

    public OpenShockWebApiBackend(PishockZapConfig config, Executor executor) {
        super(config, executor);
    }

    @Override
    protected List<Control> generateDataForOperation(ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        var devices = config.getOpenShockShockerIds();
        if (devices.isEmpty()) return List.of();
        boolean[] shocks = distributor.pickShockers(distribution, devices.size());
        var finalShocks = new ArrayList<Control>();
        for (int i = 0; i < shocks.length; i++) {
            if (!shocks[i]) continue;
            finalShocks.add(new Control(devices.get(i), ControlType.of(op), intensity, transformDuration(duration), false));
        }
        return finalShocks;
    }

    @Override
    protected @NonNull URI getUri(List<Control> data) {
        return API_URI;
    }

    @Override
    protected @NonNull Map<String, String> getHeaders(List<Control> data) {
        String token = config.getOpenShockApiToken();
        return getDefaultHeaders(token);
    }

    @Override
    protected @Nullable String getPostBody(List<Control> data) {
        return gson.toJson(Map.of(
            "shocks", data,
            "customName", config.getLogIdentifier()));
    }

    @Override
    protected void onResponse(List<Control> data, @NonNull String response) {
        if (!response.contains("Successfully sent control messages")) {
            logger.warning("OpenShock API call failed; response: " + response);
        }
    }

    @Override
    protected boolean isConfigured() {
        if (config.getOpenShockShockerIds().isEmpty()) return false;
        if (config.getOpenShockApiToken().isBlank()) return false;
        return !config.getLogIdentifier().isBlank();
    }

    private static int transformDuration(float duration) {
        return Math.round(duration * 1000.0f);
    }

    @RequiredArgsConstructor
    public static class ConnectionTest extends SimpleHttpRequestShockBackend.ConnectionTest {
        private final OpenShockWebApiConfig config;

        @Override
        public CompletableFuture<ConnectionTestResult> testConnection() {
            return testConnection(() -> gson.toJson(Map.of(
                "shocks", List.of(),
                "customName", config.getLogIdentifier())));
        }

        @Override
        public CompletableFuture<ConnectionTestResult> testVibration() {
            return testConnection(() -> {
                var devices = config.getOpenShockShockerIds();
                var finalShocks = new ArrayList<Control>();
                for (var device : devices) {
                    finalShocks.add(new Control(device, ControlType.VIBRATE, config.getVibrationIntensityMax(), transformDuration(config.getDuration()), false));
                }
                return gson.toJson(Map.of(
                    "shocks", finalShocks,
                    "customName", config.getLogIdentifier()));
            });
        }

        private CompletableFuture<ConnectionTestResult> testConnection(Supplier<String> postBody) {
            if (config.getOpenShockShockerIds().isEmpty() || config.getOpenShockApiToken().isBlank() || config.getLogIdentifier().isBlank())
                return CompletableFuture.completedFuture(ConnectionTestResult.NOT_CONFIGURED);

            return makeRequest(API_URI, getDefaultHeaders(config.getOpenShockApiToken()), postBody.get())
                .thenApply(resp -> {
                    var statusCode = resp.statusCode();
                    System.err.println("Connection test response code: " + statusCode + ", body: " + resp.body());
                    if (statusCode == 200) {
                        return ConnectionTestResult.SUCCESS;
                    } else if (statusCode == 401) {
                        return ConnectionTestResult.AUTHENTICATION_FAILED;
                    } else if (resp.body().contains("The JSON value could not be converted to System.Guid")) {
                        // Device ID is malformed
                        return ConnectionTestResult.NOT_CONFIGURED;
                    } else if (resp.body().contains("Shocker.Control.NotFound")) {
                        // Device ID is well-formed but does not exist
                        return ConnectionTestResult.DEVICE_MISSING;
                    } else {
                        return ConnectionTestResult.UNKNOWN_ERROR;
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Connection test failed; exception thrown");
                    e.printStackTrace();
                    return ConnectionTestResult.UNKNOWN_ERROR;
                });
        }
    }
}
