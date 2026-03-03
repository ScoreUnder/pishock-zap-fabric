package moe.score.pishockzap.backend;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PiShockSerialBackend implements ShockBackend {
    public static final int PISHOCK_SERIAL_BAUD_RATE = 115200;
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private final @NonNull PishockZapConfig config;
    private final @NonNull Executor executor;
    @Getter
    @NonNull
    private final String portName;
    private final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    private final Gson gson = new Gson();
    private volatile @Nullable SerialPort commPort;
    private volatile @Nullable Writer jsonWriter = null;

    public PiShockSerialBackend(@NonNull PishockZapConfig config, @NonNull Executor executor, @NonNull String portName) {
        this.config = config;
        this.executor = executor;
        this.portName = portName;
    }

    @Override
    public void performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        if (!config.isEnabled()) return;
        if (config.isVibrationOnly()) op = OpType.VIBRATE;
        if (!PiShockUtils.shockParamsAreValid(intensity, duration)) return;

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

    private @NonNull SerialPort createAndOpenPort(String portName) {
        SerialPort commPort = SerialPort.getCommPort(portName);
        commPort.setBaudRate(PISHOCK_SERIAL_BAUD_RATE);
        commPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        commPort.openPort();
        return commPort;
    }

    private @NonNull Writer openWriter() {
        Writer jsonWriter = this.jsonWriter;
        if (jsonWriter != null) return jsonWriter;

        var commPort = this.commPort = createAndOpenPort(portName);
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
        return Stream.of(SerialPort.getCommPorts()).map(SerialPort::getSystemPortName)::iterator;
    }
}
