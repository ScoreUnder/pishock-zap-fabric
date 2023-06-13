package moe.score.pishockzap;

import com.google.gson.Gson;
import moe.score.pishockzap.config.PishockZapConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PishockZapMod implements ClientModInitializer {
    public static final String NAME = "PiShock-Zap";
    private static PishockZapMod instance = null;

    public static PishockZapMod getInstance() {
        return instance;
    }

    private final Logger logger = Logger.getLogger(NAME);
    private final Path configFile = FabricLoader.getInstance().getConfigDir().resolve(NAME.toLowerCase() + ".json");
    private final PishockZapConfig config = new PishockZapConfig();

    public PishockZapConfig getConfig() {
        return config;
    }

    public void saveConfig() {
        Map<String, Object> configMap = new HashMap<>();
        config.copyToConfig(configMap);

        Gson gson = new Gson();
        try (BufferedWriter configWriter = Files.newBufferedWriter(configFile)) {
            gson.toJson(configMap, configWriter);
        } catch (IOException e) {
            logger.warning("Failed to save config file, exception details follow");
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        if (!Files.exists(configFile)) {
            logger.info("Config file not found, using default config");
            saveConfig();
            return;
        }

        Gson gson = new Gson();
        Map<String, Object> configMap;
        try {
            //noinspection unchecked
            configMap = gson.fromJson(Files.newBufferedReader(configFile), Map.class);
        } catch (Exception e) {
            logger.warning("Failed to load config file, exception details follow");
            e.printStackTrace();
            return;
        }

        config.setFromConfig(configMap);
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        loadConfig();
    }
}
