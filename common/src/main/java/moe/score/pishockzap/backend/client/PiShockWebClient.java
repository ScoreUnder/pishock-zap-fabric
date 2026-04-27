package moe.score.pishockzap.backend.client;

import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import moe.score.pishockzap.backend.model.pishock.ShareCodeInfo;
import moe.score.pishockzap.backend.model.pishock.UserDevice;
import moe.score.pishockzap.backend.model.pishock.UserProfile;
import moe.score.pishockzap.util.URIBuilder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static moe.score.pishockzap.util.Gsons.gson;
import static moe.score.pishockzap.util.Gsons.pascalCaseGson;

public class PiShockWebClient {
    private final Executor executor;
    private final HttpClient httpClient;

    public PiShockWebClient() {
        this(new CompletableFuture<Void>().defaultExecutor(), HttpClient.newBuilder().build());
    }

    public PiShockWebClient(@NonNull Executor executor, @NonNull HttpClient client) {
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
        return CompletableFuture.supplyAsync(() ->
                HttpRequest.newBuilder(new URIBuilder("https://auth.pishock.com/Auth/GetUserIfAPIKeyValid")
                    .addParameter("apikey", apiKey)
                    .addParameter("username", username)
                    .build()).build(),
            executor
        ).thenComposeAsync(
            req -> httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)),
            executor
        ).thenApplyAsync(resp -> pascalCaseGson.fromJson(resp.body(), UserProfile.class), executor);
    }

    private CompletableFuture<Map<String, List<Integer>>> getShareCodesByOwner(String apiKey, int userId) {
        return CompletableFuture.supplyAsync(() ->
                HttpRequest.newBuilder(new URIBuilder("https://ps.pishock.com/PiShock/GetShareCodesByOwner")
                    .addParameter("UserId", String.valueOf(userId))
                    .addParameter("Token", apiKey)
                    .addParameter("api", "true")
                    .build()).build(),
            executor
        ).thenComposeAsync(
            req -> httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)),
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
                URIBuilder builder = new URIBuilder("https://ps.pishock.com/PiShock/GetShockersByShareIds")
                    .addParameter("UserId", String.valueOf(userId))
                    .addParameter("Token", apiKey)
                    .addParameter("api", "true");
                for (int shareId : shareIds) {
                    builder.addParameter("shareIds", String.valueOf(shareId));
                }
                return HttpRequest.newBuilder(builder.build()).build();
            }, executor
        ).thenComposeAsync(
            req -> httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)),
            executor
        ).thenApplyAsync(
            resp -> gson.fromJson(resp.body(),
                new TypeToken<Map<String, List<ShareCodeInfo>>>() {
                }.getType()),
            executor);
    }

    public @NonNull CompletableFuture<List<UserDevice>> getUserDevices(int userId, String apiKey) {
        return CompletableFuture.supplyAsync(() ->
                HttpRequest.newBuilder(new URIBuilder("https://ps.pishock.com/PiShock/GetUserDevices")
                    .addParameter("UserId", String.valueOf(userId))
                    .addParameter("Token", apiKey)
                    .addParameter("api", "true")
                    .build()).build(),
            executor
        ).thenComposeAsync(
            req -> httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)),
            executor
        ).thenApplyAsync(resp -> gson.fromJson(resp.body(),
                new TypeToken<List<UserDevice>>() {
                }.getType()),
            executor);
    }
}
