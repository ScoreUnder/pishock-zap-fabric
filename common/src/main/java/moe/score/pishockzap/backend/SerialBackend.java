package moe.score.pishockzap.backend;

import com.fazecast.jSerialComm.SerialPort;
import lombok.NonNull;
import moe.score.pishockzap.PishockZapMod;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

@ApiStatus.Internal
public abstract class SerialBackend<D> extends SafeShockBackend {
    public static final int PISHOCK_SERIAL_BAUD_RATE = 115200;
    protected static WeakReference<SerialBackend<?>> INSTANCE;
    protected final Logger logger = Logger.getLogger(PishockZapMod.NAME);
    private final PiShockUtils.ShockDistributor distributor = new PiShockUtils.ShockDistributor();
    private final @NonNull Executor executor;
    protected String lastPortName;
    protected volatile @Nullable SerialPort commPort;
    protected volatile @Nullable Writer jsonWriter = null;

    public SerialBackend(@NonNull PishockZapConfig config, @NonNull Executor executor) {
        super(config);
        this.executor = executor;
    }

    @Override
    protected void safePerformOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        var shockers = getDevices();
        if (shockers.isEmpty()) {
            logger.warning("No shock devices configured");
            return;
        }

        boolean[] shocks = distributor.pickShockers(distribution, shockers.size());

        var lines = new ArrayList<String>();
        for (int i = 0; i < shocks.length; i++) {
            if (!shocks[i]) continue;
            var device = shockers.get(i);

            lines.add(getOperationData(op, intensity, duration, device));
        }
        if (!lines.isEmpty()) {
            doApiCallOnThread(lines);
        }
    }

    protected abstract List<D> getDevices();

    protected abstract String getOperationData(@NonNull OpType op, int intensity, float duration, D device);

    public @Nullable SerialPort reuseThisSerialPort(String serialPortAddress) {
        var commPort = this.commPort;
        return Objects.equals(lastPortName, serialPortAddress) && commPort != null ? commPort : null;
    }

    protected static @NonNull SerialPort createAndOpenPort(String portName) {
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

    private void writeLines(@NonNull Iterable<String> data) throws IOException {
        @SuppressWarnings("resource") Writer jsonWriter = openWriter();
        for (String line : data) {
            jsonWriter.write(line);
            jsonWriter.write('\n');
        }
        jsonWriter.flush();
    }

    /**
     * Perform a serial API call on a separate thread.
     *
     * @param lines data to send
     */
    protected void doApiCallOnThread(@NonNull Iterable<String> lines) {
        executor.execute(() -> {
            try {
                writeLines(lines);
            } catch (Exception e) {
                logger.warning("API call failed; exception thrown");
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
    protected static <T> CompletableFuture<T> withSerialPort(String serialPortAddress, OnConnectFunction onConnect, Function<String, Optional<T>> onLineReceived, long timeout, TimeUnit timeoutUnit) {
        var result = openAndMonitorSerialPortForLines(serialPortAddress, onConnect, onLineReceived, timeout, timeoutUnit);
        for (int retry = 0; retry < 2; retry++) {
            result = result.exceptionallyComposeAsync(e -> openAndMonitorSerialPortForLines(serialPortAddress, onConnect, onLineReceived, timeout, timeoutUnit));
        }
        return result;
    }

    private static <T> @NonNull CompletableFuture<T> openAndMonitorSerialPortForLines(String serialPortAddress, OnConnectFunction onConnect, Function<String, Optional<T>> onLineReceived, long timeout, TimeUnit timeoutUnit) {
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
            return monitorSerialPortForLines(port, serialPortIsMine, onConnect, onLineReceived, timeout, timeoutUnit);
        }).thenCompose(t -> t);
    }

    private static <T> @NonNull CompletableFuture<T> monitorSerialPortForLines(SerialPort port, boolean closeWhenDone, OnConnectFunction onConnect, Function<String, Optional<T>> lineConsumer, long timeout, TimeUnit timeoutUnit) {
        var result = new CompletableFuture<T>();
        var input = new BufferedReader(new InputStreamReader(port.getInputStream()));
        var output = new OutputStreamWriter(port.getOutputStream());

        var inputThread = new Thread(() -> {
            try {
                String line;
                while ((line = readRetrying(input, 3)) != null) {
                    System.out.println("Got from serial: " + line);
                    var lineResult = lineConsumer.apply(line);
                    if (lineResult.isPresent()) {
                        result.complete(lineResult.get());
                        break;
                    }
                }
            } catch (Exception e) {
                result.completeExceptionally(e);
                throw new RuntimeException(e);
            } catch (Throwable t) {
                result.completeExceptionally(t);
                throw t;
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
                if (!result.isDone()) {
                    result.completeExceptionally(new RuntimeException("Serial input thread terminated without completing the result"));
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();

        var outputThread = new Thread(() -> {
            try {
                onConnect.accept(output);
                output.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();

        var delayedExecutor = CompletableFuture.delayedExecutor(timeout, timeoutUnit);
        delayedExecutor.execute(inputThread::interrupt);
        delayedExecutor.execute(outputThread::interrupt);
        return result;
    }

    private static String readRetrying(BufferedReader input, int retries) throws IOException {
        try {
            return input.readLine();
        } catch (InterruptedIOException ex) {
            if (retries == 0) throw ex;
            return readRetrying(input, retries - 1);
        }
    }

    protected interface OnConnectFunction {
        void accept(OutputStreamWriter output) throws Exception;
    }
}
