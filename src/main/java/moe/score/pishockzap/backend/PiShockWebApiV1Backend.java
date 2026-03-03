package moe.score.pishockzap.backend;

import com.google.gson.Gson;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class PiShockWebApiV1Backend implements ShockBackend {
    private static final @NonNull URL API_URL;

    static {
        try {
            API_URL = URI.create("https://do.pishock.com/api/apioperate").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final PishockZapConfig config;
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    private final Executor executor;
    private final Gson gson = new Gson();

    public PiShockWebApiV1Backend(PishockZapConfig config, Executor executor) {
        this.config = config;
        this.executor = executor;
    }

    public void performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        if (!config.isEnabled()) return;
        if (config.isVibrationOnly()) op = OpType.VIBRATE;
        if (!PiShockUtils.shockParamsAreValid(intensity, duration)) return;
        if (config.getApiKey().isBlank()) {
            logger.warning("No PiShock API key configured");
            return;
        }
        if (config.getUsername().isBlank()) {
            logger.warning("No PiShock username configured");
            return;
        }

        String logIdentifier = config.getLogIdentifier();
        if (logIdentifier.isBlank()) logIdentifier = "PiShock-Zap (Minecraft)";
        else logIdentifier = logIdentifier.trim();

        String[] shockers = config.getShareCodes().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (shockers.length == 0) {
            logger.warning("No PiShock share codes configured");
            return;
        }

        boolean[] shocks = distributor.pickShockers(distribution, shockers.length);

        for (int i = 0; i < shocks.length; i++) {
            if (!shocks[i]) continue;
            String shareCode = shockers[i];

            Map<String, Object> data = new HashMap<>();
            data.put("Username", config.getUsername());
            data.put("Code", shareCode);
            data.put("Name", logIdentifier);
            data.put("Apikey", config.getApiKey());

            data.put("Op", op.code);
            data.put("Intensity", intensity);
            data.put("Duration", transformDuration(duration));

            doApiCallOnThread(data);
        }
    }

    /** Transform a floating-point duration in seconds to a PiShock API duration.
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

    /** Perform a PiShock API call on a separate thread.
     *
     * @param data data to send
     */
    private void doApiCallOnThread(Map<String, Object> data) {
        executor.execute(() -> {
            try {
                URLConnection connection = API_URL.openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                // Send data as JSON
                try (var outputStream = connection.getOutputStream()) {
                    String json = gson.toJson(data);
                    outputStream.write(json.getBytes());
                }

                // Read response and convert to string
                String response;
                try (var inputStream = connection.getInputStream()) {
                    response = new String(inputStream.readAllBytes());
                }

                // Test if successful (not reported in status code)
                // "Operation Succeeded." is the success message for v1 firmware
                // "Operation Attempted." is the success message for v3 firmware
                if (!response.contains("Operation Succeeded") && !response.contains("Operation Attempted")) {
                    logger.warning("PiShock API call failed; response: " + response);
                }
            } catch (Exception e) {
                logger.warning("PiShock API call failed; exception thrown");
                e.printStackTrace();
            }
        });
    }
}
