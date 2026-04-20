package moe.score.pishockzap.backend.impls;

import com.google.gson.Gson;
import lombok.NonNull;
import moe.score.pishockzap.backend.BulkHttpRequestShockBackend;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executor;

import static moe.score.pishockzap.util.Gsons.gson;

public class WebHookBackend extends BulkHttpRequestShockBackend<Map<String, Object>> {
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
}
