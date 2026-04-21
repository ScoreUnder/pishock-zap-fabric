package moe.score.pishockzap.backend.impls;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.PiShockUtils;
import moe.score.pishockzap.backend.SafeShockBackend;
import moe.score.pishockzap.backend.model.pishock.V2OperationType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.util.TriState;
import moe.score.pishockzap.util.URIBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static moe.score.pishockzap.util.Gsons.pascalCaseGson;

public class PiShockWebSocketApiBackend extends SafeShockBackend {
    private static final URI API_URI_BASE = URI.create("wss://broker.pishock.com/v2");
    private final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    private final Executor executor;
    private final ReadWriteLock httpLock = new ReentrantReadWriteLock();
    private HttpClient httpClient = HttpClient.newBuilder().build();
    private CompletableFuture<WebSocket> webSocketFuture;

    public PiShockWebSocketApiBackend(PishockZapConfig config, Executor executor) {
        super(config);
        this.executor = executor;
    }

    @Override
    protected void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        var devices = config.getPsHubShockers();
        var commands = new ArrayList<PublishMessage>();

        var userId = config.getPsUserId();

        int numShockers = devices.values().stream().mapToInt(List::size).sum();

        var logMetadata = new LogMetadata(userId, AccessType.API, false, false, config.getLogIdentifier());

        boolean[] shocks = distributor.pickShockers(distribution, numShockers);
        int i = 0;
        for (var hubAndShockers : devices.int2ObjectEntrySet()) {
            var hubId = hubAndShockers.getIntKey();
            var hubChannel = "c" + hubId + "-ops";
            for (var shockerId : hubAndShockers.getValue()) {
                if (!shocks[i++]) continue;
                commands.add(new PublishMessage(hubChannel, new ShockerCommand(
                    shockerId, V2OperationType.of(op), intensity, Math.round(duration * 1000), true, logMetadata)));
            }
        }

        doApiCall(new PublishCommand(commands));
    }

    @Override
    protected boolean isConfigured() {
        if (config.getPsHubShockers().isEmpty()) return false;
        if (config.getUsername().isBlank()) return false;
        if (config.getApiKey().isBlank()) return false;
        return config.getPsUserId() != -1;
    }

    @Override
    public void close() {
        var lock = httpLock.writeLock();
        lock.lock();
        try {
            if (webSocketFuture != null) {
                webSocketFuture
                    .thenComposeAsync(w -> w.sendClose(WebSocket.NORMAL_CLOSURE, ""), executor)
                    .thenAcceptAsync(WebSocket::abort, executor);
                webSocketFuture = null;
            }
            httpClient = null; // can't close() because that api is introduced later lmao
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NonNull TriState canReplaceOngoingOperation() {
        return TriState.TRUE;
    }

    private static URI getApiUri(String username, String apiKey) {
        return new URIBuilder(API_URI_BASE)
            .addParameter("Username", username)
            .addParameter("ApiKey", apiKey)
            .build();
    }

    private URI getApiUri() {
        return getApiUri(config.getUsername(), config.getApiKey());
    }

    private CompletableFuture<WebSocket> connectToApi() {
        var uri = getApiUri();
        return httpClient.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {});
    }

    private CompletableFuture<WebSocket> getWebSocketCompletableFuture() {
        var lock = httpLock.readLock();
        lock.lock();
        CompletableFuture<WebSocket> wsFuture;
        try {
            wsFuture = webSocketFuture;
        } finally {
            lock.unlock();
        }
        if (wsFuture == null) {
            var writeLock = httpLock.writeLock();
            writeLock.lock();
            try {
                wsFuture = webSocketFuture;
                if (wsFuture == null) {
                    webSocketFuture = wsFuture = connectToApi();
                }
            } finally {
                writeLock.unlock();
            }
        }
        return wsFuture;
    }

    private void whenConnected(Consumer<WebSocket> f, int retries) {
        var wsFuture = getWebSocketCompletableFuture();
        wsFuture.handleAsync((w, ex) -> {
            if (ex != null || w.isOutputClosed()) {
                if (w != null) {
                    w.abort();
                }
                var writeLock = httpLock.writeLock();
                writeLock.lock();
                try {
                    if (wsFuture == webSocketFuture) {
                        webSocketFuture = null;
                    }
                } finally {
                    writeLock.unlock();
                }
                if (retries > 0) {
                    whenConnected(f, retries - 1);
                }
            } else {
                f.accept(w);
            }
            return null;
        }, executor);
    }

    private void doApiCall(Object o) {
        whenConnected(s -> s.sendText(pascalCaseGson.toJson(o), true), 1);
    }

    static class Response {
        Object errorCode;
        boolean isError;
        String message;
        String originalCommand;
    }

    static class PingCommand {
        final String operation = "PING";
    }

    @RequiredArgsConstructor
    @SuppressWarnings("unused")
    static class PublishCommand {
        final String operation = "PUBLISH";
        final List<PublishMessage> publishCommands;
    }

    @AllArgsConstructor
    @SuppressWarnings("unused")
    static class PublishMessage {
        String target;
        ShockerCommand body;
    }

    @AllArgsConstructor
    @SuppressWarnings("unused")
    static class ShockerCommand{
        @SerializedName("id") int id;
        @SerializedName("m") V2OperationType op;
        @SerializedName("i") int intensity;
        @SerializedName("d") int duration;
        @SerializedName("r") boolean replace;
        @SerializedName("l") LogMetadata logMetadata;
    }

    @AllArgsConstructor
    @SuppressWarnings("unused")
    static class LogMetadata {
        @SerializedName("u") int userId;
        @SerializedName("ty") AccessType accessType;
        @SerializedName("w") boolean isWarning;
        @SerializedName("h") boolean isContinuous;
        @SerializedName("o") String logIdentifier;
    }

    enum AccessType {
        @SerializedName("sc") SHARE_CODE,
        @SerializedName("api") API,
    }
}
