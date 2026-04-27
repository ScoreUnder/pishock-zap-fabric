package moe.score.pishockzap.config.internal;

import java.util.List;

public interface OpenShockWebApiConfig extends ConnectionTestConfig, LoggingApiConfig, OpenShockAccountConfig {
    List<String> getOpenShockShockerIds();
}
