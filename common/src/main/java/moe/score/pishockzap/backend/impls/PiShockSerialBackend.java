package moe.score.pishockzap.backend.impls;

import com.fazecast.jSerialComm.SerialPort;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.SerialBackend;
import moe.score.pishockzap.backend.model.pishock.V2OperationType;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.util.TriState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static moe.score.pishockzap.util.Gsons.gson;

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
            logger.warning("No PiShock shocker IDs configured");
            return false;
        }
        if (config.getSerialPort().isBlank()) {
            logger.warning("No serial port configured");
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
    private int transformDuration(float duration) {
        return Math.round(duration * 1000.0f);
    }

    @NonNull
    public static CompletableFuture<List<Integer>> probeDeviceIds(String serialPortAddress) {
        return CompletableFuture.supplyAsync(() -> {
            WeakReference<SerialBackend<?>> instanceRef = SerialBackend.INSTANCE;
            SerialBackend<?> existingInstance = instanceRef == null ? null : instanceRef.get();
            boolean serialPortIsMine;
            SerialPort port = existingInstance == null ? null : existingInstance.reuseThisSerialPort(serialPortAddress);
            if (port == null) {
                System.out.println("Opening my own serial port");
                port = createAndOpenPort(serialPortAddress);
                serialPortIsMine = true;
            } else {
                System.out.println("Reusing a serial port");
                serialPortIsMine = false;
            }
            return getDeviceIdsFromPort(port, serialPortIsMine);
        }).thenCompose(t -> t);
    }

    private static @NonNull CompletableFuture<List<Integer>> getDeviceIdsFromPort(SerialPort port, boolean closeWhenDone) {
        var result = new CompletableFuture<List<Integer>>();
        var input = new BufferedReader(new InputStreamReader(port.getInputStream()));
        var output = new OutputStreamWriter(port.getOutputStream());

        var ioThread = new Thread(() -> {
            try {
                output.write("\nG:I\n");
                output.flush();

                String prefix = "TERMINALINFO:";
                String line;
                while ((line = input.readLine()) != null) {
                    System.out.println("Got from serial: " + line);
                    if (line.startsWith(prefix)) {
                        var terminalInfo = gson.fromJson(line.substring(prefix.length()), TerminalInfo.class);
                        result.complete(
                            terminalInfo.shockers.stream().map(s -> s.id).toList());
                    }
                }
            } catch (Exception e) {
                result.completeExceptionally(e);
                throw new RuntimeException(e);
            } finally {
                if (closeWhenDone) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            port.closePort();
                        }
                    }
                }
            }
        });
        ioThread.setDaemon(true);
        ioThread.start();

        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(ioThread::interrupt);
        return result;
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
}
