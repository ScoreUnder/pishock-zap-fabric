package moe.score.pishockzap.pishockapi;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PiShockSerialApi implements PiShockApi {
    public static final int PISHOCK_SERIAL_BAUD_RATE = 115200;
    private final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private final PishockZapConfig config;
    private final Executor executor;
    @Getter
    @NonNull
    private final String portName;
    private final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    private final Gson gson = new Gson();
    private SerialPort commPort;
    private Writer jsonWriter = null;

    public PiShockSerialApi(@NonNull PishockZapConfig config, @NonNull Executor executor, @NonNull String portName) {
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

        for (int i = 0; i < shocks.length; i++) {
            if (!shocks[i]) continue;
            int deviceId = shockers.get(i);

            Map<String, Object> data = new HashMap<>();
            data.put("cmd", "operate");
            Map<String, Object> params = new HashMap<>();
            data.put("value", params);

            params.put("id", deviceId);
            params.put("op", op.firmwareCode);
            params.put("intensity", intensity);
            params.put("duration", transformDuration(duration));

            doApiCallOnThread(data);
        }
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
        return (int) Math.round(duration * 1000.0f);
    }

    /**
     * Perform a PiShock API call on a separate thread.
     *
     * @param data data to send
     */
    private void doApiCallOnThread(Map<String, Object> data) {
        executor.execute(() -> {
            try {
                if (commPort == null) {
                    commPort = SerialPort.getCommPort(portName);
                    commPort.setBaudRate(PISHOCK_SERIAL_BAUD_RATE);
                    commPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
                    commPort.openPort();

                    jsonWriter = new OutputStreamWriter(commPort.getOutputStream());
                }
                jsonWriter.write(gson.toJson(data));
                jsonWriter.write('\n');
                jsonWriter.flush();
            } catch (Exception e) {
                logger.warning("PiShock API call failed; exception thrown");
                e.printStackTrace();

                var commPort = this.commPort;
                if (commPort != null) {
                    this.commPort = null;
                    this.jsonWriter = null;
                    commPort.closePort();
                }
            }
        });
    }

    @Override
    public void close() {
        if (commPort != null) {
            commPort.closePort();
            commPort = null;
        }
    }

    @NonNull
    public static Iterable<String> getSerialPorts() {
        return Stream.of(SerialPort.getCommPorts()).map(SerialPort::getSystemPortName)::iterator;
    }
}
