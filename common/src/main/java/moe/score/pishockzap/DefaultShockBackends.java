package moe.score.pishockzap;

import moe.score.pishockzap.backend.ShockBackendRegistry;
import moe.score.pishockzap.backend.impls.*;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class DefaultShockBackends {
    public static final String PISHOCK_WEB_V1 = "pishock_web_v1";
    public static final String PISHOCK_WEBSOCKET = "pishock_websocket";
    public static final String PISHOCK_SERIAL = "pishock_serial";
    public static final String WEBHOOK = "webhook";
    public static final String OPENSHOCK_WEB = "openshock_web";
    public static final String OPENSHOCK_SERIAL = "openshock_serial";

    public static void registerAll() {
        ShockBackendRegistry.register(PISHOCK_WEBSOCKET, "enum.pishock-zap.config.api_type.websocket", PiShockWebSocketApiBackend::new);
        ShockBackendRegistry.register(PISHOCK_SERIAL, "enum.pishock-zap.config.api_type.serial", PiShockSerialBackend::new);
        ShockBackendRegistry.register(WEBHOOK, "enum.pishock-zap.config.api_type.webhook", WebHookBackend::new);
        ShockBackendRegistry.register(OPENSHOCK_WEB, "enum.pishock-zap.config.api_type.openshock", OpenShockWebApiBackend::new);
        ShockBackendRegistry.register(OPENSHOCK_SERIAL, "enum.pishock-zap.config.api_type.openshock.serial", OpenShockSerialBackend::new);
    }
}
