package moe.score.pishockzap.backend.impls;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.backend.BulkHttpRequestShockBackend;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.util.HttpUtil;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class OpenShockWebApiBackend extends BulkHttpRequestShockBackend<List<OpenShockWebApiBackend.Control>> {
    private static final URL API_URL;
    private static final URL API_MY_DEVICES_URL;
    private static final Gson gson = new Gson();
    private static String userAgent;

    static {
        try {
            API_URL = new URL("https://api.openshock.app/2/shockers/control");
            API_MY_DEVICES_URL = new URL("https://api.openshock.app/1/shockers/own");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

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
            finalShocks.add(new Control(devices.get(i), ControlType.of(op), intensity, transformDuration(duration), false));
        }
        return finalShocks;
    }

    @Override
    protected @NonNull URL getUrl(List<Control> data) {
        return API_URL;
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
    protected byte @Nullable [] getPostBody(List<Control> data) {
        return gson.toJson(Map.of(
                        "shocks", data,
                        "customName", config.getLogIdentifier()))
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void onResponse(List<Control> data, byte[] response) {
        var result = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(response)).toString();
        if (!result.contains("Successfully sent control messages")) {
            logger.warning("OpenShock API call failed; response: " + result);
        }
    }

    @Override
    protected boolean isConfigured() {
        if (config.getOpenShockShockerIds().isEmpty()) return false;
        if (config.getOpenShockApiToken().isBlank()) return false;
        return true;
    }

    private int transformDuration(float duration) {
        return Math.round(duration * 1000.0f);
    }

    public static CompletableFuture<List<String>> probeDeviceIds(String apiToken) {
        return HttpUtil.makeRequestAsyncUtf8(() -> API_MY_DEVICES_URL, null, () -> getDefaultHeaders(apiToken))
                .thenApply(result -> {
                    ResponseMessage<List<Hub>> response = gson.fromJson(
                            result,
                            new TypeToken<ResponseMessage<List<Hub>>>() {
                            }.getType());
                    if (response.data == null || (response.data.isEmpty() && !response.message.isBlank())) {
                        throw new RuntimeException("Error from OpenShock API: " + response.message);
                    }
                    return response.data.stream().flatMap(h -> h.shockers.stream()).map(Shocker::getId).toList();
                });
    }

    public record Control(String id, ControlType type, int intensity, int duration, boolean exclusive) {
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

    @Data
    public static class ResponseMessage<T> {
        String message;
        T data;
    }

    @Data
    public static class Hub {
        List<Shocker> shockers = List.of();
        String id;
        String name;
        String createdOn;
    }

    @Data
    public static class Shocker {
        String name;
        boolean isPaused;
        String createdOn;
        String id;
        int rfId;
        String model;
    }
}
