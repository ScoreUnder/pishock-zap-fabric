package moe.score.pishockzap.backend.impls;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.backend.BackendConnectionTest;
import moe.score.pishockzap.backend.ConnectionTestResult;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SerialBackend;
import moe.score.pishockzap.backend.model.pishock.V2OperationType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.internal.PiShockSerialConfig;
import moe.score.pishockzap.util.TriState;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static moe.score.pishockzap.util.Gsons.gson;

@Slf4j(topic = Constants.NAME)
public class PiShockSerialBackend extends SerialBackend<Integer> {
    public PiShockSerialBackend(@NonNull PishockZapConfig config, @NonNull Executor executor) {
        super(config, executor);
    }

    @Override
    protected boolean areShockParamsValid(OpType op, int intensity, float duration) {
        return intensity >= 0 && intensity <= 100 && duration > 0 && duration < getMaxDuration();
    }

    @Override
    public float getMaxDuration() {
        return 2147483.5f;
    }

    @Override
    public boolean isConfigured() {
        if (config.getDeviceIds().isEmpty()) {
            log.warn("No PiShock shocker IDs configured");
            return false;
        }
        if (config.getSerialPort().isBlank()) {
            log.warn("No serial port configured");
            return false;
        }
        return true;
    }

    @Override
    public @NonNull TriState canReplaceOngoingOperation() {
        return TriState.TRUE;
    }

    @Override
    protected List<Integer> getDevices() {
        return config.getDeviceIds();
    }

    @Override
    protected @NonNull String getOperationData(@NonNull OpType op, int intensity, float duration, Integer deviceId) {
        return getOperateCommandString(op, intensity, duration, deviceId);
    }

    static String getOperateCommandString(@NonNull OpType op, int intensity, float duration, Integer deviceId) {
        var operateCommand = new OperateCommand(new OperatePayload(deviceId, V2OperationType.of(op), intensity, transformDuration(duration)));
        return gson.toJson(operateCommand);
    }

    @RequiredArgsConstructor
    @SuppressWarnings("unused")
    public static class OperateCommand {
        private final String cmd = "operate";
        private final OperatePayload value;
    }

    public record OperatePayload(int id, V2OperationType op, int intensity, int duration) {
    }

    /**
     * Transform a floating-point duration in seconds to a PiShock Serial API duration.
     * <p>
     * This is just a simple sec -> ms conversion.
     *
     * @param duration duration in seconds
     * @return duration in PiShock API format
     */
    private static int transformDuration(float duration) {
        return Math.round(duration * 1000.0f);
    }

    @NonNull
    public static CompletableFuture<List<Integer>> probeDeviceIds(String serialPortAddress) {
        return SerialBackend.withSerialPort(serialPortAddress, output -> output.write("\nG:I\n"), line -> {
            String prefix = "TERMINALINFO:";
            if (line.startsWith(prefix)) {
                var terminalInfo = gson.fromJson(line.substring(prefix.length()), TerminalInfo.class);
                return Optional.of(terminalInfo.shockers.stream().map(s -> s.id).toList());
            }
            return Optional.empty();
        }, 3, TimeUnit.SECONDS);
    }

    @NoArgsConstructor
    private static class TerminalInfo {
        String version;
        int type;
        boolean connected;
        int clientId;
        String wifi;
        String server;
        String macAddress;
        List<Shocker> shockers = List.of();
        List<Network> networks = List.of();
        String otk;
        boolean claimed;
        boolean isDev;
        boolean publisher;
        boolean polled;
        boolean subscriber;
        String publicIp;
        boolean internet;
        int ownerId;
    }

    @NoArgsConstructor
    private static class Shocker {
        int id;
        int type;
        boolean paused;
    }

    @NoArgsConstructor
    private static class Network {
        String ssid;
        String password;
    }

    @RequiredArgsConstructor
    public static class ConnectionTest implements BackendConnectionTest {
        private final PiShockSerialConfig config;

        public CompletableFuture<ConnectionTestResult> testConnection() {
            return testConnection(
                output -> output.write("\nG:I\n"),
                line -> {
                    if (line.startsWith("TERMINALINFO:")) {
                        return Optional.of(ConnectionTestResult.SUCCESS);
                    }
                    return Optional.empty();
                });
        }

        private CompletableFuture<ConnectionTestResult> testConnection(OnConnectFunction onConnect, Function<String, Optional<ConnectionTestResult>> onLineReceived) {
            if (config.getSerialPort().isBlank() || config.getDeviceIds().isEmpty()) {
                return CompletableFuture.completedFuture(ConnectionTestResult.NOT_CONFIGURED);
            }
            return SerialBackend.withSerialPort(config.getSerialPort(), onConnect, onLineReceived, 3, TimeUnit.SECONDS)
                .exceptionally(t -> t instanceof InterruptedException ? ConnectionTestResult.TIMED_OUT : ConnectionTestResult.CONNECTION_FAILED);
        }

        public CompletableFuture<ConnectionTestResult> testVibration() {
            return testConnection(
                output -> {
                    for (var device : config.getDeviceIds()) {
                        output.write(getOperateCommandString(OpType.VIBRATE, config.getVibrationIntensityMax(), config.getDuration(), device));
                        output.write('\n');
                    }
                },
                line -> {
                    if (line.startsWith("Received JSON:")) {
                        return Optional.of(ConnectionTestResult.SUCCESS);
                    }
                    return Optional.empty();
                });
        }
    }
}
