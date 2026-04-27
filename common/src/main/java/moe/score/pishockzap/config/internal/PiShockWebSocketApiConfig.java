package moe.score.pishockzap.config.internal;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;

public interface PiShockWebSocketApiConfig extends ConnectionTestConfig, LoggingApiConfig, PiShockAccountConfig {
    Int2ObjectMap<IntList> getPsHubShockers();

    int getPsUserId();
}
