package moe.score.pishockzap.pishockapi;

import com.google.gson.Gson;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class WebHookApi implements PiShockApi {
    private final PishockZapConfig config;
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private final Executor executor;
    private final Gson gson = new Gson();

    public WebHookApi(PishockZapConfig config, Executor executor) {
        this.config = config;
        this.executor = executor;
    }

    @Override
    public void performOp(ShockDistribution distribution, OpType op, int intensity, float duration) {
        if (!config.isEnabled()) return;
        if (config.isVibrationOnly()) op = OpType.VIBRATE;
        if (!PiShockUtils.shockParamsAreValid(intensity, duration)) return;

        Map<String, Object> data = new HashMap<>();
        data.put("operation", op.name());
        data.put("intensity", intensity);
        data.put("duration", duration);
        data.put("distribution", distribution.name());

        doApiCallOnThread(data);
    }

    private void doApiCallOnThread(Map<String, Object> data) {
        executor.execute(() -> {
            try {
                URLConnection connection = new URL(config.getCustomWebhookUrl()).openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                // Send data as JSON
                try (var outputStream = connection.getOutputStream()) {
                    String json = gson.toJson(data);
                    outputStream.write(json.getBytes());
                }

                connection.getInputStream().close();
                logger.info("Custom Webhook call successful, data: " + gson.toJson(data));
            } catch (Exception e) {
                logger.warning("Custom Webhook call failed; exception thrown");
                e.printStackTrace();
            }
        });
    }
}
