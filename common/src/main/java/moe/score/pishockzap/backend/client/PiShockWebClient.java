package moe.score.pishockzap.backend.client;

import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.backend.model.pishock.api.ShockerInfo;
import moe.score.pishockzap.backend.model.pishock.auth.GetAccountResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static moe.score.pishockzap.util.Gsons.gson;

/// PiShock web API client
/// see [PiShock Auth API](https://auth.pishock.com/swagger/v0/swagger.json)
/// and [PiShock API](https://api.pishock.com/swagger/v0/swagger.json)
@Slf4j(topic = Constants.NAME)
public class PiShockWebClient {
    private static final String PISHOCK_API_KEY_HEADER = "X-PiShock-Api-Key";
    // Note: there's an "api." version of this too. Not sure when to prefer each one.
    private static final URI PISHOCK_GET_ACCOUNT_URI = URI.create("https://auth.pishock.com/Account");
    private static final URI PISHOCK_GET_SHOCKERS_URI = URI.create("https://api.pishock.com/Shockers");

    private final Executor executor;
    private final HttpClient httpClient;

    public PiShockWebClient() {
        this(new CompletableFuture<Void>().defaultExecutor(), HttpClient.newBuilder().build());
    }

    public PiShockWebClient(@NonNull Executor executor, @NonNull HttpClient client) {
        this.executor = executor;
        this.httpClient = client;
    }

    private <T> CompletableFuture<T> get(URI uri, String apiKey, String operation, TypeToken<T> type) {
        HttpRequest req;
        try {
            req = HttpRequest.newBuilder(uri).header(PISHOCK_API_KEY_HEADER, apiKey).build();
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .whenComplete((resp, err) -> {
                if (err != null) {
                    log.warn("Error from {}", operation, err);
                } else {
                    log.debug("Response from {} (status {} on {}): {}", operation, resp.statusCode(), resp.request().uri(), resp.body());
                }
            })
            .thenApply(resp -> {
                var statusCode = resp.statusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    throw new RuntimeException("Failed to " + operation + ": " + statusCode + " " + resp.body());
                }
                return resp;
            })
            .thenApplyAsync(resp -> gson.fromJson(resp.body(), type.getType()), executor);
    }

    public CompletableFuture<GetAccountResponse> getUserAccount(String apiKey) {
        return get(PISHOCK_GET_ACCOUNT_URI, apiKey, "get user account", new TypeToken<>() {
        });
    }

    public @NonNull CompletableFuture<List<ShockerInfo>> getShockers(String apiKey) {
        return get(PISHOCK_GET_SHOCKERS_URI, apiKey, "get shockers", new TypeToken<>() {
        });
    }
}
