package moe.score.pishockzap.backend.impls;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import lombok.NonNull;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SimpleHttpRequestShockBackend;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class PiShockWebApiV1Backend extends SimpleHttpRequestShockBackend<String, Map<String, Object>> {
    private static final @NonNull URL API_URL;
    private static final Map<String, String> API_HEADERS = ImmutableMap.of("Content-Type", "application/json");
    private static final Gson gson = new Gson();

    static {
        try {
            API_URL = URI.create("https://do.pishock.com/api/apioperate").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public PiShockWebApiV1Backend(PishockZapConfig config, Executor executor) {
        super(config, executor);
    }

    @Override
    protected Map<String, Object> generateDataForOperation(String shareCode, @NonNull OpType op, int intensity, float duration) {
        return Map.of(
            "Username", config.getUsername(),
            "Code", shareCode,
            "Name", config.getLogIdentifier(),
            "Apikey", config.getApiKey(),
            "Op", op.code,
            "Intensity", intensity,
            "Duration", transformDuration(duration));
    }

    @Override
    protected @NonNull URL getUrl(Map<String, Object> data) {
        return API_URL;
    }

    @Override
    protected @NonNull Map<String, String> getHeaders(Map<String, Object> data) {
        return API_HEADERS;
    }

    @Override
    protected byte @Nullable [] getPostBody(Map<String, Object> data) {
        return gson.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void onResponse(Map<String, Object> data, byte[] response) {
        var responseStr = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(response)).toString();

        // Test if successful (not reported in status code)
        // "Operation Succeeded." is the success message for v1 firmware
        // "Operation Attempted." is the success message for v3 firmware
        if (!responseStr.contains("Operation Succeeded") && !responseStr.contains("Operation Attempted")) {
            logger.warning("PiShock API call failed; response: " + responseStr);
        }
    }

    @Override
    public boolean isConfigured() {
        if (config.getApiKey().isBlank()) {
            logger.warning("No PiShock API key configured");
            return false;
        }
        if (config.getUsername().isBlank()) {
            logger.warning("No PiShock username configured");
            return false;
        }
        if (config.getShareCodes().isEmpty()) {
            logger.warning("No PiShock share codes configured");
            return false;
        }
        return true;
    }

    @Override
    protected @NonNull List<String> getDevices() {
        return config.getShareCodes();
    }

    @Override
    public @NonNull TriState canReplaceOngoingOperation() {
        return TriState.FALSE;
    }

    /**
     * Transform a floating-point duration in seconds to a PiShock API duration.
     * <p>
     * PiShock API duration can be specified one of two ways: as an integer number of seconds,
     * or if greater than 100, as an integer number of milliseconds.
     * <p>
     * Since the firmware natively uses milliseconds now, and the API now fully supports longer
     * (>1.5s) durations when specified as milliseconds, we will always use milliseconds.
     *
     * @param duration duration in seconds
     * @return duration in PiShock API format
     */
    private int transformDuration(float duration) {
        float durationMs = duration * 1000.0f;
        if (durationMs <= 100) {
            // The API can't handle durations less than 100ms
            // (those are treated as amounts of seconds, which could be disastrous)
            return 0;
        }
        return Math.round(durationMs);
    }
}
