package moe.score.pishockzap.pishockapi;

import com.google.gson.Gson;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class PiShockApi {
    public static final int PISHOCK_MAX_DURATION = 15;
    public static final int PISHOCK_MAX_INTENSITY = 100;
    private static final URL API_URL;

    static {
        try {
            API_URL = URI.create("https://do.pishock.com/api/apioperate").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private final PishockZapConfig config;
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private final Random random = new Random();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int roundRobinIndex = 0;
    private final Gson gson = new Gson();

    public PiShockApi(PishockZapConfig config) {
        this.config = config;
    }

    public void performOp(ShockDistribution distribution, OpType op, int intensity, float duration) {
        if (!config.isEnabled()) return;
        if (config.isVibrationOnly()) op = OpType.VIBRATE;
        if (intensity == 0 || duration == 0.0f) return;
        if (intensity < 0 || intensity > PISHOCK_MAX_INTENSITY) {
            logger.warning("PiShock intensity out of range: " + intensity);
            return;
        }
        if (duration < 0.0f || duration > PISHOCK_MAX_DURATION) {
            logger.warning("PiShock duration out of range: " + duration);
            return;
        }
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

        boolean[] shocks = pickShockers(distribution, shockers.length);

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

    /** Pick from a list of shockers by distribution.
     *
     * @param distribution distribution to use
     * @param length number of shockers to pick from
     * @return a boolean array of length {@code length} with {@code true} for each shocker to shock
     */
    private boolean @NotNull [] pickShockers(ShockDistribution distribution, int length) {
        boolean[] shocks = new boolean[length];
        int randomIndex = random.nextInt(length);
        if (roundRobinIndex >= length) roundRobinIndex = 0;

        for (int i = 0; i < length; i++) {
            shocks[i] = switch (distribution) {
                case ALL -> true;
                case ROUND_ROBIN -> i == roundRobinIndex;
                case RANDOM -> i == randomIndex;
                case RANDOM_ALL -> random.nextBoolean();
                case FIRST -> i == 0;
                case LAST -> i == shocks.length - 1;
            };
        }

        roundRobinIndex++;

        if (distribution == ShockDistribution.RANDOM_ALL) {
            boolean hasShock = false;
            for (boolean shock : shocks) {
                if (shock) {
                    hasShock = true;
                    break;
                }
            }
            // If no shocks were selected, select a random one
            if (!hasShock) shocks[randomIndex] = true;
        }
        return shocks;
    }

    /** Transform a floating-point duration in seconds to a PiShock API duration.
     * <p>
     * PiShock API duration can be specified one of two ways: as an integer number of seconds,
     * or if greater than 100, as an integer number of milliseconds.
     * However, firmware only goes in 100ms increments, so we give a round multiple of 100ms.
     *
     * @param duration duration in seconds
     * @return duration in PiShock API format
     */
    private int transformDuration(float duration) {
        int rounded = (int) duration * 10;
        if (rounded % 10 == 0) return rounded / 10;
        return rounded * 100;
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
                // "Operation Succeeded." is the success message
                if (!response.contains("Succe")) {
                    logger.warning("PiShock API call failed; response: " + response);
                }
            } catch (Exception e) {
                logger.warning("PiShock API call failed; exception thrown");
                e.printStackTrace();
            }
        });
    }

    public void teardown() {
        executor.shutdown();
    }
}
