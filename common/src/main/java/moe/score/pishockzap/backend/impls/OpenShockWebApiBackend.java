package moe.score.pishockzap.backend.impls;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.*;
import lombok.experimental.Accessors;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.backend.BulkHttpRequestShockBackend;
import moe.score.pishockzap.backend.ConnectionTestResult;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SimpleHttpRequestShockBackend;
import moe.score.pishockzap.backend.model.openshock.ShockCollarModel;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.config.internal.OpenShockWebApiConfig;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static moe.score.pishockzap.util.Gsons.gson;

public class OpenShockWebApiBackend extends BulkHttpRequestShockBackend<List<OpenShockWebApiBackend.Control>> {
    private static final URI API_URI = URI.create("https://api.openshock.app/2/shockers/control");
    private static final URI API_MY_DEVICES_URI = URI.create("https://api.openshock.app/1/shockers/own");
    private static String userAgent;

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

    private static @NonNull Map<String, String> getDefaultHeaders(String token) {
        if (userAgent == null) {
            userAgent = PishockZapMod.NAME + "/" + PishockZapMod.getVersion() + " (minecraft mod; github.com/ScoreUnder/pishock-zap-fabric)";
        }
        return Map.of(
            "User-Agent", userAgent,
            "Content-Type", "application/json",
            "Open-Shock-Token", token);
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
        return true;
    }

    private static int transformDuration(float duration) {
        return Math.round(duration * 1000.0f);
    }

    public static CompletableFuture<List<String>> probeDeviceIds(String apiToken) {
        return probeDevices(apiToken).thenApply(ds -> ds.stream().map(d -> d.id).toList());
    }

    public static CompletableFuture<List<Shocker>> probeDevices(String apiToken) {
        var executor = new CompletableFuture<Void>().defaultExecutor();
        @SuppressWarnings("resource")
        var httpClient = HttpClient.newBuilder().executor(executor).build();
        var req = HttpRequest.newBuilder(API_MY_DEVICES_URI);
        for (var header : getDefaultHeaders(apiToken).entrySet()) {
            req.setHeader(header.getKey(), header.getValue());
        }
        return httpClient.sendAsync(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(result -> {
                ResponseMessage<List<Hub>> response = gson.fromJson(
                    result.body(),
                    new TypeToken<ResponseMessage<List<Hub>>>() {
                    }.getType());
                if (response.data == null || (response.data.isEmpty() && !response.message.isBlank())) {
                    throw new RuntimeException("Error from OpenShock API: " + response.message);
                }
                return response.data.stream().flatMap(h -> h.shockers.stream()).toList();
            });
    }

    @AllArgsConstructor
    public static class Control {
        String id;
        ControlType type;
        int intensity;
        int duration;
        boolean exclusive;
    }

    public enum ControlType {
        @SerializedName("Stop") STOP,
        @SerializedName("Shock") SHOCK,
        @SerializedName("Vibrate") VIBRATE,
        @SerializedName("Sound") SOUND;

        public static ControlType of(OpType op) {
            return switch (op) {
                case SHOCK -> SHOCK;
                case BEEP -> SOUND;
                case VIBRATE -> VIBRATE;
            };
        }
    }

    @NoArgsConstructor
    public static class ResponseMessage<T> {
        String message;
        T data;
    }

    @NoArgsConstructor
    public static class Hub {
        List<Shocker> shockers = List.of();
        String id;
        String name;
        String createdOn;
    }

    @NoArgsConstructor
    @Getter
    @Accessors(fluent = true)
    public static class Shocker {
        String name;
        boolean isPaused;
        String createdOn;
        String id;
        int rfId;
        ShockCollarModel model;
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
