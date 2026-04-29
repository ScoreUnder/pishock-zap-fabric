package moe.score.pishockzap.backend.impls;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.backend.*;
import moe.score.pishockzap.backend.model.pishock.V2OperationType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.config.internal.PiShockWebSocketApiConfig;
import moe.score.pishockzap.util.TriState;
import moe.score.pishockzap.util.URIBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

        int numShockers = devices.values().stream().mapToInt(List::size).sum();

        var logMetadata = makeLogMetadata(config);

        boolean[] shocks = distributor.pickShockers(distribution, numShockers);
        int i = 0;
        for (var hubAndShockers : devices.int2ObjectEntrySet()) {
            var hubChannel = makeHubChannel(hubAndShockers.getIntKey());
            for (var shockerId : hubAndShockers.getValue()) {
                if (!shocks[i++]) continue;
                addShockerCommand(commands, op, intensity, duration, shockerId, hubChannel, logMetadata);
            }
        }

        var publishCommand = new PublishCommand(commands);
        doApiCall(publishCommand);
    }

    static @NonNull String makeHubChannel(int hubId) {
        return "c" + hubId + "-ops";
    }

    static @NonNull LogMetadata makeLogMetadata(PiShockWebSocketApiConfig config) {
        return new LogMetadata(config.getPsUserId(), AccessType.API, false, false, config.getLogIdentifier());
    }

    static void addShockerCommand(ArrayList<PublishMessage> commands, @NonNull OpType op, int intensity, float duration, int shockerId, String hubChannel, LogMetadata logMetadata) {
        commands.add(new PublishMessage(hubChannel, new ShockerCommand(
            shockerId, V2OperationType.of(op), intensity, Math.round(duration * 1000), true, logMetadata)));
    }

    @Override
    protected boolean isConfigured() {
        if (config.getPsHubShockers().isEmpty()) return false;
        if (config.getUsername().isBlank()) return false;
        if (config.getApiKey().isBlank()) return false;
        return config.getPsUserId() != -1;
    }

    @Override
    public void onWorldJoin() {
        doApiCall(new PingCommand());
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
        return httpClient.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {
        });
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
        String errorCode;
        boolean isError;
        String message;
        String originalCommand;
        Object source;

        @UtilityClass
        static class ErrorCodes {
            public static final String AUTH_ERROR = "AUTH_ERROR";
            public static final String USER_ID_MISMATCH = "USER_ID_MISMATCH";
            public static final String PERMISSION_ERROR = "PERMISSION_ERROR";
            public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
            public static final String API_KEY_ERROR = "API_KEY_ERROR";
        }
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
    static class ShockerCommand {
        @SerializedName("id")
        int id;
        @SerializedName("m")
        V2OperationType op;
        @SerializedName("i")
        int intensity;
        @SerializedName("d")
        int duration;
        @SerializedName("r")
        boolean replace;
        @SerializedName("l")
        LogMetadata logMetadata;
    }

    @AllArgsConstructor
    @SuppressWarnings("unused")
    static class LogMetadata {
        @SerializedName("u")
        int userId;
        @SerializedName("ty")
        AccessType accessType;
        @SerializedName("w")
        boolean isWarning;
        @SerializedName("h")
        boolean isContinuous;
        @SerializedName("o")
        String logIdentifier;
    }

    enum AccessType {
        @SerializedName("sc") SHARE_CODE,
        @SerializedName("api") API,
    }

    @RequiredArgsConstructor
    @Slf4j(topic = Constants.NAME)
    public static class ConnectionTest implements BackendConnectionTest {
        private final PiShockWebSocketApiConfig config;
        private final HttpClient httpClient = HttpClient.newBuilder().build();

        @Override
        public CompletableFuture<ConnectionTestResult> testConnection() {
            return testConnection(PingCommand::new, response -> checkError(response).or(() -> {
                if (response.originalCommand.contains("PING")) {
                    return Optional.of(ConnectionTestResult.SUCCESS);
                }
                return Optional.empty();
            }));
        }

        @Override
        public CompletableFuture<ConnectionTestResult> testVibration() {
            return testConnection(() -> {
                var devices = config.getPsHubShockers();
                var commands = new ArrayList<PublishMessage>();

                var logMetadata = makeLogMetadata(config);

                for (var hubAndShockers : devices.int2ObjectEntrySet()) {
                    var hubChannel = makeHubChannel(hubAndShockers.getIntKey());
                    for (var shockerId : hubAndShockers.getValue()) {
                        addShockerCommand(commands, OpType.VIBRATE, config.getVibrationIntensityMax(), config.getDuration(), shockerId, hubChannel, logMetadata);
                    }
                }

                return new PublishCommand(commands);
            }, response -> checkError(response).or(() -> {
                if (response.originalCommand.contains("PUBLISH")) {
                    return Optional.of(ConnectionTestResult.SUCCESS);
                }
                return Optional.empty();
            }));
        }

        private @NonNull CompletableFuture<ConnectionTestResult> testConnection(Supplier<Object> commandSupplier, Function<Response, Optional<ConnectionTestResult>> onResponse) {
            if (config.getPsHubShockers().isEmpty() || config.getUsername().isBlank() || config.getApiKey().isBlank() || config.getPsUserId() == -1 || config.getLogIdentifier().isBlank()) {
                return CompletableFuture.completedFuture(ConnectionTestResult.NOT_CONFIGURED);
            }

            CompletableFuture<ConnectionTestResult> resultFuture = new CompletableFuture<>();

            var uri = getApiUri(config.getUsername(), config.getApiKey());
            var wsFuture = httpClient.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    log.trace("WebSocket closed: {} {}", statusCode, reason);
                    resultFuture.complete(ConnectionTestResult.CONNECTION_FAILED);
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    log.trace("WebSocket error", error);
                    resultFuture.complete(ConnectionTestResult.CONNECTION_FAILED);
                    WebSocket.Listener.super.onError(webSocket, error);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    log.trace("WEBSOCKET RESPONSE: {}", data);
                    var result = onResponse.apply(pascalCaseGson.fromJson(data.toString(), Response.class));
                    result.ifPresent(resultFuture::complete);
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }
            });
            wsFuture.whenComplete((sock, ex) -> {
                if (ex == null) {
                    sock.sendText(pascalCaseGson.toJson(commandSupplier.get()), true);
                } else {
                    log.warn("Failed to connect to WebSocket", ex);
                    resultFuture.complete(ConnectionTestResult.CONNECTION_FAILED);
                }
            });

            CompletableFuture.delayedExecutor(10, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> resultFuture.complete(ConnectionTestResult.TIMED_OUT));

            return resultFuture.whenComplete((x, ex) ->
                wsFuture.thenAccept(w -> {
                    w.sendClose(WebSocket.NORMAL_CLOSURE, "");
                    CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS)
                        .execute(w::abort);
                }));
        }

        private static Optional<ConnectionTestResult> checkError(Response response) {
            if (!response.isError) {
                return Optional.empty();
            }
            return switch (response.errorCode) {
                case Response.ErrorCodes.AUTH_ERROR, Response.ErrorCodes.API_KEY_ERROR ->
                    Optional.of(ConnectionTestResult.AUTHENTICATION_FAILED);
                case Response.ErrorCodes.USER_ID_MISMATCH, Response.ErrorCodes.PERMISSION_DENIED,
                     Response.ErrorCodes.PERMISSION_ERROR -> Optional.of(ConnectionTestResult.PERMISSION_DENIED);
                default -> Optional.of(ConnectionTestResult.UNKNOWN_ERROR);
            };
        }
    }
}
