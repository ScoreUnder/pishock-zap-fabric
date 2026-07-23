package moe.score.pishockzap.config;

import com.google.gson.JsonObject;
import lombok.NonNull;
import moe.score.pishockzap.DefaultShockBackends;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static moe.score.pishockzap.config.ConfigSerialiser.VERSION;
import static moe.score.pishockzap.config.ConfigSerialiser.VERSION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PishockZapConfigTest {

    private static @NonNull PishockZapConfig jsonToConfig(JsonObject oldConfigData) {
        var config = new PishockZapConfig();
        ConfigSerialiser.updateConfigFromJson(config, oldConfigData);
        return config;
    }

    @Test
    void oldConfigIsMigrated() {
        var oldConfigData = new JsonObject();

        oldConfigData.addProperty("vibrationThreshold", 8);
        oldConfigData.addProperty("maxDamage", 16);

        var config = jsonToConfig(oldConfigData);

        assertEquals(0.4f, config.getVibrationThreshold(), 0.0001f);
        assertEquals(0.8f, config.getMaxDamage(), 0.0001f);

        var newConfigData = ConfigSerialiser.configToJson(config);

        assertEquals(0.4f, newConfigData.get("vibrationThreshold").getAsFloat(), 0.0001f);
        assertEquals(0.8f, newConfigData.get("maxDamage").getAsFloat(), 0.0001f);
        assertEquals(VERSION, newConfigData.get(VERSION_KEY).getAsInt());
    }

    @Test
    void v1ConfigIsNotMigrated() {
        var oldConfigData = new JsonObject();

        oldConfigData.addProperty("vibrationThreshold", 0.5f);
        oldConfigData.addProperty("maxDamage", 0.75f);
        oldConfigData.addProperty(VERSION_KEY, 1.0);

        var config = jsonToConfig(oldConfigData);

        assertEquals(0.5f, config.getVibrationThreshold(), 0.0001f);
        assertEquals(0.75f, config.getMaxDamage(), 0.0001f);

        var newConfigData = ConfigSerialiser.configToJson(config);

        assertEquals(0.5f, newConfigData.get("vibrationThreshold").getAsFloat(), 0.0001f);
        assertEquals(0.75f, newConfigData.get("maxDamage").getAsFloat(), 0.0001f);
        assertEquals(VERSION, newConfigData.get(VERSION_KEY).getAsInt());
    }

    static Stream<Arguments> provideV1LocalSerialConfigData() {
        return Stream.of(
            Arguments.of(true, DefaultShockBackends.PISHOCK_SERIAL),
            Arguments.of(false, DefaultShockBackends.PISHOCK_WEB_V1)
        );
    }

    static Stream<Arguments> provideV2LocalSerialConfigData() {
        return Stream.of(
            Arguments.of("SERIAL", DefaultShockBackends.PISHOCK_SERIAL),
            Arguments.of("WEBHOOK", DefaultShockBackends.WEBHOOK),
            Arguments.of("OPENSHOCK", DefaultShockBackends.OPENSHOCK_WEB),
            Arguments.of("WEB_V1", DefaultShockBackends.PISHOCK_WEB_V1),
            Arguments.of("oopsie", DefaultShockBackends.PISHOCK_WEB_V1)
        );
    }

    @ParameterizedTest
    @MethodSource("provideV1LocalSerialConfigData")
    void v1ConfigLocalIsMigrated(boolean localEnabled, String expectedApiType) {
        var oldConfigData = new JsonObject();

        oldConfigData.addProperty("localEnabled", localEnabled);
        oldConfigData.addProperty(VERSION_KEY, 1.0);

        var config = jsonToConfig(oldConfigData);

        assertEquals(expectedApiType, config.getApiType());

        var newConfigData = ConfigSerialiser.configToJson(config);

        assertEquals(VERSION, newConfigData.get(VERSION_KEY).getAsInt());
    }

    @ParameterizedTest
    @MethodSource("provideV2LocalSerialConfigData")
    void v2ConfigApiTypeIsMigrated(String oldApiType, String expectedApiType) {
        var oldConfigData = new JsonObject();

        oldConfigData.addProperty("apiType", oldApiType);
        oldConfigData.addProperty(VERSION_KEY, 2.0);

        var config = jsonToConfig(oldConfigData);

        assertEquals(expectedApiType, config.getApiType());

        var newConfigData = ConfigSerialiser.configToJson(config);

        assertEquals(VERSION, newConfigData.get(VERSION_KEY).getAsInt());
    }

    @ParameterizedTest
    @MethodSource("provideV2LocalSerialConfigData")
    void v3ConfigApiTypeIsNotMigrated(String ignored, String expectedApiType) {
        var oldConfigData = new JsonObject();

        oldConfigData.addProperty("apiType", expectedApiType);
        oldConfigData.addProperty(VERSION_KEY, 3.0);

        var config = jsonToConfig(oldConfigData);

        assertEquals(expectedApiType, config.getApiType());

        var newConfigData = ConfigSerialiser.configToJson(config);

        assertEquals(VERSION, newConfigData.get(VERSION_KEY).getAsInt());
    }
}
