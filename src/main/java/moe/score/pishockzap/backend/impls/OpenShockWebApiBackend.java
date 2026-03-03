package moe.score.pishockzap.backend.impls;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.backend.BulkHttpRequestShockBackend;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class OpenShockWebApiBackend extends BulkHttpRequestShockBackend<List<OpenShockWebApiBackend.Control>> {
    private static final URL API_URL;
    private static final Gson gson = new Gson();
    private String userAgent;

    static {
        try {
            API_URL = new URL("https://api.openshock.app/2/shockers/control");
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
        if (userAgent == null) {
            userAgent = PishockZapMod.NAME + "/" + PishockZapMod.getVersion() + " (minecraft mod; github.com/ScoreUnder/pishock-zap-fabric)";
        }
        return Map.of(
            "User-Agent", PishockZapMod.NAME + "/" + PishockZapMod.getVersion(),
            "Content-Type", "application/json",
            "Open-Shock-Token", config.getOpenShockApiToken());
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
}
