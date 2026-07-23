package moe.score.pishockzap.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.Constants;
import moe.score.pishockzap.DefaultShockBackends;
import moe.score.pishockzap.util.Gsons;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ApiStatus.Internal
@UtilityClass
@Slf4j(topic = Constants.NAME)
public class ConfigSerialiser {
    static final String VERSION_KEY = "CONFIG_VERSION_DO_NOT_EDIT";

    private static final List<Consumer<JsonObject>> MIGRATIONS = List.of(
        config -> {
            // Migrate from integer damage equivalent to float damage equivalent
            if (config.get("vibrationThreshold") instanceof JsonPrimitive vibrationThreshold && vibrationThreshold.isNumber()) {
                config.addProperty("vibrationThreshold", vibrationThreshold.getAsFloat() * 0.05f);
            }
            if (config.get("maxDamage") instanceof JsonPrimitive maxDamage && maxDamage.isNumber()) {
                config.addProperty("maxDamage", maxDamage.getAsFloat() * 0.05f);
            }
        },
        config -> {
            // Migrate from localEnabled to API type enum
            if (config.get("localEnabled") instanceof JsonPrimitive localEnabled && localEnabled.isBoolean()) {
                config.addProperty("apiType", localEnabled.getAsBoolean() ? "SERIAL" : "WEB_V1");
                config.remove("localEnabled");
            }
        },
        config -> {
            // Migrate from API type enum to registry
            if (config.get("apiType") instanceof JsonPrimitive s && s.isString()) {
                config.addProperty("apiType", switch (s.getAsString()) {
                    case "SERIAL" -> DefaultShockBackends.PISHOCK_SERIAL;
                    case "WEBHOOK" -> DefaultShockBackends.WEBHOOK;
                    case "OPENSHOCK" -> DefaultShockBackends.OPENSHOCK_WEB;
                    default -> DefaultShockBackends.PISHOCK_WEB_V1;
                });
            }
        }
    );

    static final int VERSION = MIGRATIONS.size();

    private static void migrateConfig(@NonNull JsonObject config) {
        int version = 0;
        if (config.get(VERSION_KEY) instanceof JsonPrimitive versionJson && versionJson.isNumber()) {
            version = Math.max(0, versionJson.getAsInt());
        }

        while (version < MIGRATIONS.size()) {
            MIGRATIONS.get(version).accept(config);
            version++;
        }
    }

    @SuppressWarnings("LoggingSimilarMessage")
    private static void sanitiseNulls(@NonNull JsonElement value, Supplier<StringBuilder> path) {
        // how could this possibly ever come back to bite me :)
        if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            for (var entry : List.copyOf(object.entrySet())) {
                var key = entry.getKey();
                var child = entry.getValue();
                Supplier<StringBuilder> newPath = () -> path.get().append(".").append(key);
                if (child.isJsonNull()) {
                    log.warn("Removing null value at path: {}", newPath.get());
                    object.remove(key);
                } else {
                    sanitiseNulls(child, newPath);
                }
            }
        } else if (value.isJsonArray()) {
            var array = value.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                int finalI = i;
                Supplier<StringBuilder> newPath = () -> path.get().append("[").append(finalI).append("]");
                var child = array.get(i);
                if (child.isJsonNull()) {
                    log.warn("Removing null value at path: {}", newPath.get());
                    array.remove(i);
                    i--;
                } else {
                    sanitiseNulls(child, newPath);
                }
            }
        }
    }

    public static void updateConfigFromJson(@NonNull PishockZapConfig config, @NonNull JsonObject json) {
        migrateConfig(json);
        sanitiseNulls(json, () -> new StringBuilder().append("config"));
        var newConfig = Gsons.prettyGson.fromJson(json, config.getClass());
        config.updateFrom(newConfig);
    }

    public static void writeConfig(@NonNull PishockZapConfig config, @NonNull Appendable out) {
        Gsons.prettyGson.toJson(configToJson(config), out);
    }

    public static @NonNull JsonObject configToJson(@NonNull PishockZapConfig config) {
        var json = Gsons.prettyGson.toJsonTree(config).getAsJsonObject();
        json.addProperty(VERSION_KEY, VERSION);
        return json;
    }
}
