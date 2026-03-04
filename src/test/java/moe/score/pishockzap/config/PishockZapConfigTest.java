package moe.score.pishockzap.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.stream.Stream;

import static moe.score.pishockzap.config.PishockZapConfig.CONFIG_VERSION;
import static moe.score.pishockzap.config.PishockZapConfig.CONFIG_VERSION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PishockZapConfigTest {
    @Test
    void oldConfigIsMigrated() {
        var oldConfigData = new HashMap<String, Object>();

        oldConfigData.put("vibrationThreshold", 8);
        oldConfigData.put("maxDamage", 16);

        var config = new PishockZapConfig();
        config.setFromConfig(oldConfigData);

        assertEquals(0.4f, config.getVibrationThreshold(), 0.0001f);
        assertEquals(0.8f, config.getMaxDamage(), 0.0001f);

        var newConfigData = new HashMap<String, Object>();
        config.copyToConfig(newConfigData);

        assertEquals(0.4f, (Float) newConfigData.get("vibrationThreshold"), 0.0001f);
        assertEquals(0.8f, (Float) newConfigData.get("maxDamage"), 0.0001f);
        assertEquals(CONFIG_VERSION, newConfigData.get(CONFIG_VERSION_KEY));
    }

    @Test
    void v1ConfigIsNotMigrated() {
        var oldConfigData = new HashMap<String, Object>();

        oldConfigData.put("vibrationThreshold", 0.5f);
        oldConfigData.put("maxDamage", 0.75f);
        oldConfigData.put(CONFIG_VERSION_KEY, 1.0);

        var config = new PishockZapConfig();
        config.setFromConfig(oldConfigData);

        assertEquals(0.5f, config.getVibrationThreshold(), 0.0001f);
        assertEquals(0.75f, config.getMaxDamage(), 0.0001f);

        var newConfigData = new HashMap<String, Object>();
        config.copyToConfig(newConfigData);

        assertEquals(0.5f, (Float) newConfigData.get("vibrationThreshold"), 0.0001f);
        assertEquals(0.75f, (Float) newConfigData.get("maxDamage"), 0.0001f);
        assertEquals(CONFIG_VERSION, newConfigData.get(CONFIG_VERSION_KEY));
    }

    static Stream<Arguments> provideV1LocalSerialConfigData() {
        return Stream.of(
            Arguments.of(true, ShockBackendType.SERIAL),
            Arguments.of(false, ShockBackendType.WEB_V1)
        );
    }

    @ParameterizedTest
    @MethodSource("provideV1LocalSerialConfigData")
    void v1ConfigLocalIsMigrated(boolean localEnabled, ShockBackendType expectedApiType) {
        var oldConfigData = new HashMap<String, Object>();

        oldConfigData.put("localEnabled", localEnabled);
        oldConfigData.put(CONFIG_VERSION_KEY, 1.0);

        var config = new PishockZapConfig();
        config.setFromConfig(oldConfigData);

        assertEquals(expectedApiType, config.getApiType());

        var newConfigData = new HashMap<String, Object>();
        config.copyToConfig(newConfigData);

        assertEquals(expectedApiType.name(), newConfigData.get("apiType"));
        assertEquals(CONFIG_VERSION, newConfigData.get(CONFIG_VERSION_KEY));
    }

    @ParameterizedTest
    @MethodSource("provideV1LocalSerialConfigData")
    void v2ConfigApiTypeIsNotMigrated(boolean ignored, ShockBackendType expectedApiType) {
        var oldConfigData = new HashMap<String, Object>();

        oldConfigData.put("apiType", expectedApiType.name());
        oldConfigData.put(CONFIG_VERSION_KEY, 2.0);

        var config = new PishockZapConfig();
        config.setFromConfig(oldConfigData);

        assertEquals(expectedApiType, config.getApiType());

        var newConfigData = new HashMap<String, Object>();
        config.copyToConfig(newConfigData);

        assertEquals(expectedApiType.name(), newConfigData.get("apiType"));
        assertEquals(CONFIG_VERSION, newConfigData.get(CONFIG_VERSION_KEY));
    }
}
