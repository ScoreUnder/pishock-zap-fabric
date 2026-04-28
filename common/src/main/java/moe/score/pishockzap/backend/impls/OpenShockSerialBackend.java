package moe.score.pishockzap.backend.impls;

import com.google.gson.annotations.SerializedName;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.backend.BackendConnectionTest;
import moe.score.pishockzap.backend.ConnectionTestResult;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SerialBackend;
import moe.score.pishockzap.backend.model.openshock.ShockCollarModel;
import moe.score.pishockzap.backend.model.openshock.ShockDevice;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.internal.OpenShockSerialConfig;
import moe.score.pishockzap.util.TriState;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static moe.score.pishockzap.util.Gsons.gson;

public class OpenShockSerialBackend extends SerialBackend<ShockDevice> {
    public OpenShockSerialBackend(@NonNull PishockZapConfig config, @NonNull Executor executor) {
        super(config, executor);
    }

    @Override
    protected boolean areShockParamsValid(OpType op, int intensity, float duration) {
        return intensity >= 0 && intensity <= 255 && duration > 0 && duration < getMaxDuration();
    }

    @Override
    public float getMaxDuration() {
        return 65.535f;
    }

    @Override
    public boolean isConfigured() {
        if (getDevices().isEmpty()) {
            logger.warning("No shocker IDs configured");
            return false;
        }
        if (config.getSerialPort().isBlank()) {
            logger.warning("No serial port configured");
            return false;
        }
        return true;
    }

    @Override
    protected List<ShockDevice> getDevices() {
        return config.getOpenShockSerialDevices();
    }

    @Override
    protected String getOperationData(@NonNull OpType op, int intensity, float duration, ShockDevice device) {
        return getRfTransmitCommandString(op, intensity, duration, device);
    }

    @Override
    public @NonNull TriState canReplaceOngoingOperation() {
        return TriState.TRUE;
    }

    static String getRfTransmitCommandString(@NonNull OpType op, int intensity, float duration, ShockDevice device) {
        var payload = new RfTransmitPayload(
            device.model(), device.id(),
            ShockerCommandType.of(op), intensity, Math.round(duration * 1000));
        // longest valid string without extra spacing:
        // "rftransmit {"model":"petrainer998dr","id":65535,"type":"vibrate","intensity":255,"durationMs":65535}"
        // = 100 chars long
        var sb = new StringBuilder(100);
        sb.append("rftransmit ");
        gson.toJson(payload, sb);
        return sb.toString();
    }

    public record RfTransmitPayload(
        ShockCollarModel model, int id, ShockerCommandType type, int intensity, int durationMs) {
    }

    public enum ShockerCommandType {
        @SerializedName("shock") SHOCK,
        @SerializedName("vibrate") VIBRATE,
        @SerializedName("sound") SOUND,
        @SerializedName("light") LIGHT,
        @SerializedName("stop") STOP;

        public static ShockerCommandType of(OpType op) {
            return switch (op) {
                case SHOCK -> SHOCK;
                case VIBRATE -> VIBRATE;
                case BEEP -> SOUND;
            };
        }
    }

    @RequiredArgsConstructor
    public static class ConnectionTest implements BackendConnectionTest {
        private final OpenShockSerialConfig config;

        public CompletableFuture<ConnectionTestResult> testConnection() {
            return testConnection(
                output -> output.write("\njsonconfig\n"),
                line -> {
                    if (line.startsWith("$SYS$|Response|JsonConfig|")) {
                        return Optional.of(ConnectionTestResult.SUCCESS);
                    }
                    return Optional.empty();
                });
        }

        private CompletableFuture<ConnectionTestResult> testConnection(OnConnectFunction onConnect, Function<String, Optional<ConnectionTestResult>> onLineReceived) {
            if (config.getSerialPort().isBlank() || config.getOpenShockSerialDevices().isEmpty()) {
                return CompletableFuture.completedFuture(ConnectionTestResult.NOT_CONFIGURED);
            }
            return SerialBackend.withSerialPort(config.getSerialPort(), onConnect, onLineReceived, 5, TimeUnit.SECONDS)
                .exceptionally(t -> ConnectionTestResult.CONNECTION_FAILED);
        }

        public CompletableFuture<ConnectionTestResult> testVibration() {
            return testConnection(
                output -> {
                    for (var device : config.getOpenShockSerialDevices()) {
                        output.write(getRfTransmitCommandString(OpType.VIBRATE, config.getVibrationIntensityMax(), config.getDuration(), device));
                        output.write('\n');
                    }
                },
                line -> {
                    if (line.startsWith("$SYS$|Success") || line.contains("[RFTransmitter] Command received")) {
                        return Optional.of(ConnectionTestResult.SUCCESS);
                    }
                    return Optional.empty();
                });
        }
    }
}
