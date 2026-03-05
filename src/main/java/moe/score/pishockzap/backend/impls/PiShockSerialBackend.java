package moe.score.pishockzap.backend.impls;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import lombok.Data;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.backend.OpType;
import moe.score.pishockzap.backend.PiShockUtils;
import moe.score.pishockzap.backend.SafeShockBackend;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PiShockSerialBackend extends SafeShockBackend {
    public static final int PISHOCK_SERIAL_BAUD_RATE = 115200;
    private static WeakReference<PiShockSerialBackend> INSTANCE;
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private static final Gson gson = new Gson();
    private final @NonNull Executor executor;
    private String lastPortName;
    private final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    private volatile @Nullable SerialPort commPort;
    private volatile @Nullable Writer jsonWriter = null;

    public PiShockSerialBackend(@NonNull PishockZapConfig config, @NonNull Executor executor) {
        super(config);
        this.executor = executor;
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
    protected void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        List<Integer> shockers = config.getDeviceIds();
        if (shockers.isEmpty()) {
            logger.warning("No PiShock shocker IDs configured");
            return;
        }

        boolean[] shocks = distributor.pickShockers(distribution, shockers.size());

        var lines = new ArrayList<String>();
        for (int i = 0; i < shocks.length; i++) {
            if (!shocks[i]) continue;
            int deviceId = shockers.get(i);

            Map<String, Object> data = getOperationData(op, intensity, duration, deviceId);
            String line = convertToJson(data);
            lines.add(line);
        }
        if (!lines.isEmpty()) {
            doApiCallOnThread(lines);
        }
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

    private @NonNull Map<String, Object> getOperationData(@NonNull OpType op, int intensity, float duration, int deviceId) {
        Map<String, Object> data = new HashMap<>();
        data.put("cmd", "operate");
        Map<String, Object> params = new HashMap<>();
        data.put("value", params);

        params.put("id", deviceId);
        params.put("op", op.firmwareCode);
        params.put("intensity", intensity);
        params.put("duration", transformDuration(duration));
        return data;
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

    private static @NonNull SerialPort createAndOpenPort(String portName) {
        SerialPort commPort = SerialPort.getCommPort(portName);
        commPort.setBaudRate(PISHOCK_SERIAL_BAUD_RATE);
        commPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
        commPort.openPort();
        return commPort;
    }

    private @NonNull Writer openWriter() {
        var instance = INSTANCE;
        if (instance == null || instance.get() != this) {
            INSTANCE = new WeakReference<>(this);
        }
        if (!Objects.equals(lastPortName, config.getSerialPort())) {
            lastPortName = config.getSerialPort();
            close();
        }
        Writer jsonWriter = this.jsonWriter;
        if (jsonWriter != null) return jsonWriter;

        var commPort = this.commPort = createAndOpenPort(lastPortName);
        jsonWriter = this.jsonWriter = new OutputStreamWriter(commPort.getOutputStream());
        return jsonWriter;
    }

    private String convertToJson(@NonNull Map<String, Object> data) {
        return gson.toJson(data);
    }

    private void writeLines(@NonNull Iterable<String> data) throws IOException {
        @SuppressWarnings("resource") Writer jsonWriter = openWriter();
        for (String line : data) {
            jsonWriter.write(line);
            jsonWriter.write('\n');
        }
        jsonWriter.flush();
    }

    /**
     * Perform a PiShock API call on a separate thread.
     *
     * @param lines data to send
     */
    private void doApiCallOnThread(@NonNull Iterable<String> lines) {
        executor.execute(() -> {
            try {
                writeLines(lines);
            } catch (Exception e) {
                logger.warning("PiShock API call failed; exception thrown");
                e.printStackTrace();

                close();
            }
        });
    }

    @Override
    public void close() {
        var commPort = this.commPort;
        if (commPort != null) {
            this.commPort = null;
            this.jsonWriter = null;
            commPort.closePort();
        }
    }

    @NonNull
    public static Iterable<String> getSerialPorts() {
        return Stream.of(SerialPort.getCommPorts()).map(SerialPort::getSystemPortPath)::iterator;
    }

    @NonNull
    public static CompletableFuture<List<Integer>> probeDeviceIds(String serialPortAddress) {
        WeakReference<PiShockSerialBackend> instanceRef = INSTANCE;
        PiShockSerialBackend existingInstance = instanceRef == null ? null : instanceRef.get();
        boolean serialPortIsMine;
        SerialPort port;
        if (existingInstance == null || !Objects.equals(existingInstance.lastPortName, serialPortAddress) || existingInstance.commPort == null) {
            System.out.println("Opening my own serial port");
            port = createAndOpenPort(serialPortAddress);
            serialPortIsMine = true;
        } else {
            System.out.println("Reusing a serial port");
            port = Objects.requireNonNull(existingInstance.commPort);
            serialPortIsMine = false;
        }

        return getDeviceIdsFromPort(port, serialPortIsMine);
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
                            terminalInfo.shockers.stream().map(Shocker::getId).toList());
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

    @Data
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

    @Data
    private static class Shocker {
        int id;
        int type;
        boolean paused;
    }

    @Data
    private static class Network {
        String ssid;
        String password;
    }
}
