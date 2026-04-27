package moe.score.pishockzap.backend.client;

import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.backend.model.openshock.Hub;
import moe.score.pishockzap.backend.model.openshock.ResponseMessage;
import moe.score.pishockzap.backend.model.openshock.Shocker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static moe.score.pishockzap.util.Gsons.gson;

public class OpenShockWebClient {
    private static final URI API_MY_DEVICES_URI = URI.create("https://api.openshock.app/1/shockers/own");
    private static String userAgent;

    public static @NonNull Map<String, String> getDefaultHeaders(String token) {
        if (userAgent == null) {
            userAgent = PishockZapMod.NAME + "/" + PishockZapMod.getVersion() + " (minecraft mod; github.com/ScoreUnder/pishock-zap-fabric)";
        }
        return Map.of(
            "User-Agent", userAgent,
            "Content-Type", "application/json",
            "Open-Shock-Token", token);
    }

    public static CompletableFuture<List<String>> probeDeviceIds(String apiToken) {
        return probeDevices(apiToken).thenApply(ds -> ds.stream().map(Shocker::id).toList());
    }

    public static CompletableFuture<List<Shocker>> probeDevices(String apiToken) {
        var executor = new CompletableFuture<Void>().defaultExecutor();
        @SuppressWarnings("resource")
        var httpClient = HttpClient.newBuilder().executor(executor).build();
        var req = HttpRequest.newBuilder(API_MY_DEVICES_URI);
        for (var header : getDefaultHeaders(apiToken).entrySet()) {
            req.setHeader(header.getKey(), header.getValue());
        }
        return httpClient.sendAsync(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(result -> {
                ResponseMessage<List<Hub>> response = gson.fromJson(
                    result.body(),
                    new TypeToken<ResponseMessage<List<Hub>>>() {
                    }.getType());
                if (response.data() == null || (response.data().isEmpty() && !response.message().isBlank())) {
                    throw new RuntimeException("Error from OpenShock API: " + response.message());
                }
                return response.data().stream().flatMap(h -> h.shockers().stream()).toList();
            });
    }
}
