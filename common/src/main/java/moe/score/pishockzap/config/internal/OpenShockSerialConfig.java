package moe.score.pishockzap.config.internal;

import moe.score.pishockzap.backend.model.openshock.ShockDevice;

import java.util.List;

public interface OpenShockSerialConfig extends SerialConfig {
    List<ShockDevice> getOpenShockSerialDevices();
}
