package moe.score.pishockzap.backend.impls;

import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SimpleHttpRequestShockBackend;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.util.TriState;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static moe.score.pishockzap.util.Gsons.gson;
import static moe.score.pishockzap.util.Gsons.pascalCaseGson;

public class PiShockWebApiV1Backend extends SimpleHttpRequestShockBackend<String, PiShockWebApiV1Backend.ShockerOperation> {
    private static final @NonNull URI API_URI = URI.create("https://do.pishock.com/api/apioperate");
    private static final Map<String, String> API_HEADERS = ImmutableMap.of("Content-Type", "application/json");
    private static final EnumMap<OpType, Integer> API_CODE_BY_OP = new EnumMap<>(OpType.class);

    static {
        API_CODE_BY_OP.put(OpType.SHOCK, 0);
        API_CODE_BY_OP.put(OpType.VIBRATE, 1);
        API_CODE_BY_OP.put(OpType.BEEP, 2);
    }

    public PiShockWebApiV1Backend(PishockZapConfig config, Executor executor) {
        super(config, executor);
    }

    @Override
    protected ShockerOperation generateDataForOperation(String shareCode, @NonNull OpType op, int intensity, float duration) {
        return new ShockerOperation(config.getUsername(), shareCode, config.getLogIdentifier(), config.getApiKey(),
                API_CODE_BY_OP.get(op), intensity, transformDuration(duration));
    }

    @Override
    protected @NonNull URI getUri(ShockerOperation data) {
        return API_URI;
    }

    @Override
    protected @NonNull Map<String, String> getHeaders(ShockerOperation data) {
        return API_HEADERS;
    }

    @Override
    protected @Nullable String getPostBody(ShockerOperation data) {
        return pascalCaseGson.toJson(data);
    }

    @Override
    protected void onResponse(ShockerOperation data, @NonNull String response) {
        // Test if successful (not reported in status code)
        // "Operation Succeeded." is the success message for v1 firmware
        // "Operation Attempted." is the success message for v3 firmware
        if (!response.contains("Operation Succeeded") && !response.contains("Operation Attempted")) {
            logger.warning("PiShock API call failed; response: " + response);
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

    public static class HttpBackend {
        private final Executor executor;
        private final HttpClient httpClient;

        public HttpBackend() {
            this(new CompletableFuture<Void>().defaultExecutor(), HttpClient.newBuilder().build());
        }

        public HttpBackend(@NonNull Executor executor, @NonNull HttpClient client) {
            this.executor = executor;
            this.httpClient = client;
        }

        public CompletableFuture<List<String>> probeShareCodes(String username, String apiKey) {
            return getUserProfile(username, apiKey).thenComposeAsync(profile ->
                            getShareCodesByOwner(apiKey, profile.userId).thenComposeAsync(shareIdsMap -> {
                                List<Integer> myShareIds = shareIdsMap.get(profile.username);
                                List<Integer> shareIds = myShareIds == null || myShareIds.isEmpty()
                                        ? shareIdsMap.values().stream().flatMap(List::stream).toList()
                                        : shareIdsMap.get(profile.username);

                                return getShockersByShareIds(apiKey, profile.userId, shareIds);
                            }, executor), executor)
                    .thenApplyAsync(shockersMap -> shockersMap.values().stream()
                            .flatMap(List::stream)
                            .map(info -> info.shareCode)
                            .toList(), executor);
        }

        public CompletableFuture<UserProfile> getUserProfile(String username, String apiKey) {
            return CompletableFuture.supplyAsync(() -> {
                        try {
                            return HttpRequest.newBuilder(new URIBuilder("https://auth.pishock.com/Auth/GetUserIfAPIKeyValid")
                                    .addParameter("apikey", apiKey)
                                    .addParameter("username", username)
                                    .build()).build();
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    executor
            ).thenComposeAsync(
                    req -> httpClient.sendAsync(req, BodyHandlers.ofString(StandardCharsets.UTF_8)),
                    executor
            ).thenApplyAsync(resp -> pascalCaseGson.fromJson(resp.body(), UserProfile.class), executor);
        }

        private CompletableFuture<Map<String, List<Integer>>> getShareCodesByOwner(String apiKey, int userId) {
            return CompletableFuture.supplyAsync(() -> {
                        try {
                            return HttpRequest.newBuilder(new URIBuilder("https://ps.pishock.com/PiShock/GetShareCodesByOwner")
                                    .addParameter("UserId", String.valueOf(userId))
                                    .addParameter("Token", apiKey)
                                    .addParameter("api", "true")
                                    .build()).build();
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    executor
            ).thenComposeAsync(
                    req -> httpClient.sendAsync(req, BodyHandlers.ofString(StandardCharsets.UTF_8)),
                    executor
            ).thenApplyAsync(
                    resp -> gson.fromJson(resp.body(),
                            new TypeToken<Map<String, List<Integer>>>() {
                            }.getType()),
                    executor);
        }

        private @NonNull CompletableFuture<Map<String, List<ShareCodeInfo>>> getShockersByShareIds(String apiKey, int userId, List<Integer> shareIds) {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            URIBuilder builder = new URIBuilder("https://ps.pishock.com/PiShock/GetShockersByShareIds")
                                    .addParameter("UserId", String.valueOf(userId))
                                    .addParameter("Token", apiKey)
                                    .addParameter("api", "true");
                            for (int shareId : shareIds) {
                                builder.addParameter("shareIds", String.valueOf(shareId));
                            }
                            return HttpRequest.newBuilder(builder.build()).build();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, executor
            ).thenComposeAsync(
                    req -> httpClient.sendAsync(req, BodyHandlers.ofString(StandardCharsets.UTF_8)),
                    executor
            ).thenApplyAsync(
                    resp -> gson.fromJson(resp.body(),
                            new TypeToken<Map<String, List<ShareCodeInfo>>>() {
                            }.getType()),
                    executor);
        }

        public @NonNull CompletableFuture<List<UserDevice>> getUserDevices(int userId, String apiKey) {
            return CompletableFuture.supplyAsync(() -> {
                    try {
                        return HttpRequest.newBuilder(new URIBuilder("https://ps.pishock.com/PiShock/GetUserDevices")
                            .addParameter("UserId", String.valueOf(userId))
                            .addParameter("Token", apiKey)
                            .addParameter("api", "true")
                            .build()).build();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                },
                executor
            ).thenComposeAsync(
                req -> httpClient.sendAsync(req, BodyHandlers.ofString(StandardCharsets.UTF_8)),
                executor
            ).thenApplyAsync(resp -> gson.fromJson(resp.body(),
                    new TypeToken<List<UserDevice>>() {
                    }.getType()),
                executor);
        }
    }

    @NoArgsConstructor
    public static class UserProfile {
        public int userId;
        public String username;
        public String lastLogin;
        public String password;
        @SerializedName("IPAddress")
        public String ipAddress;
        public Object sessions;
        public Object emails;
        @SerializedName("APIKeys")
        public List<ApiKey> apiKeys;
        public Object oAuthLinks;
        public Object images;
        public Object accessPermissions;
    }

    @NoArgsConstructor
    private static class ApiKey {
        @SerializedName("UserAPIKeyId")
        public int userApiKeyId;
        public Object user;
        @SerializedName("APIKey")
        public String apiKey;
        public String name;
        public String expiry;
        public String generated;
        public Object scopes;
    }

    @NoArgsConstructor
    private static class ShareCodeInfo {
        public int shareId;
        public int clientId;
        public int shockerId;
        public String shockerName;
        public boolean isPaused;
        public int maxIntensity;
        public boolean canContinuous;
        public boolean canShock;
        public boolean canVibrate;
        public boolean canBeep;
        public boolean canLog;
        public String shareCode;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    protected static class ShockerOperation {
        public String username;
        public String code;
        public String name;
        @SerializedName("Apikey")
        public String apiKey;
        public int op;
        public int intensity;
        public int duration;
    }

    @NoArgsConstructor
    public static class UserDevice {
        public int clientId;
        public String name;
        public int userId;
        public String username;
        public List<Shocker> shockers;
    }

    @NoArgsConstructor
    public static class Shocker {
        public String name;
        public int shockerId;
        public boolean isPaused;
    }
}
