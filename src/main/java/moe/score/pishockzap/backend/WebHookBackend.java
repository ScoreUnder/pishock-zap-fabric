package moe.score.pishockzap.backend;

import com.google.gson.Gson;
import lombok.NonNull;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class WebHookBackend extends SimpleHttpRequestShockBackend<ShockDistribution, Map<String, Object>> {
    private static final Gson gson = new Gson();

    public WebHookBackend(PishockZapConfig config, Executor executor) {
        super(config, executor);
    }

    @Override
    protected Map<String, Object> generateDataForOperation(ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        return Map.of(
            "operation", op.name(),
            "intensity", intensity,
            "duration", duration,
            "distribution", distribution.name());
    }

    @Override
    protected @NonNull URL getUrl(Map<String, Object> data) throws MalformedURLException {
        return new URL(config.getCustomWebhookUrl());
    }

    @Override
    protected @NonNull Map<String, String> getHeaders(Map<String, Object> data) {
        return Map.of();
    }

    @Override
    protected byte @Nullable [] getPostBody(Map<String, Object> data) {
        return gson.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void onResponse(Map<String, Object> data, byte[] response) {
        logger.info("Custom Webhook call successful, request: " + gson.toJson(data) + "\nresponse: " + StandardCharsets.UTF_8.decode(ByteBuffer.wrap(response)));
    }

    @Override
    protected @NonNull List<ShockDistribution> getDevices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConfigured() {
        try {
            getUrl(Map.of());
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        doApiCallOnThread(generateDataForOperation(distribution, op, intensity, duration));
    }
}
