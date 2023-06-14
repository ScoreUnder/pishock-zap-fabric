package moe.score.pishockzap;

import com.google.gson.Gson;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.PiShockApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static moe.score.pishockzap.ZapController.MAX_DAMAGE;

public class PishockZapMod implements ClientModInitializer {
    public static final String NAME = "PiShock-Zap";
    private static PishockZapMod instance = null;

    public static PishockZapMod getInstance() {
        return instance;
    }

    private final Logger logger = Logger.getLogger(NAME);
    private final Path configFile = FabricLoader.getInstance().getConfigDir().resolve(NAME.toLowerCase() + ".json");
    private final PishockZapConfig config = new PishockZapConfig();
    private final PlayerHpWatcher playerHpWatcher = new PlayerHpWatcher();
    private final ZapController zapController = new ZapController(new PiShockApi(config), config);

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

    public void onPlayerHpChange() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator() || player.isCreative()) {
            // Don't zap spectators or creative players
            playerHpWatcher.resetPlayer();
            return;
        }

        int hp = Math.round(player.getHealth());
        hp = Math.max(0, Math.min(hp, MAX_DAMAGE));

        int damage = playerHpWatcher.updatePlayerHpAndGetDamage(player, hp);
        if (damage > 0) {
            boolean deathZap = hp == 0;
            MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.of("Death? " + deathZap + " Damage: " + damage + ", hp: " + hp), false);
            logger.info("Death? " + deathZap + ", damage: " + damage + ", hp: " + hp);
            ShockDistribution distribution = deathZap && config.isShockOnDeath() ? config.getShockDistributionDeath() : config.getShockDistribution();
            int damageEquivalent = config.isShockOnHealth() ? MAX_DAMAGE - hp : damage;
            if (damageEquivalent > MAX_DAMAGE) {
                logger.warning("Damage equivalent " + damageEquivalent + " exceeds max damage " + MAX_DAMAGE + ", capping");
                damageEquivalent = MAX_DAMAGE;
            }
            zapController.queueShock(distribution, deathZap, damageEquivalent, config.getDuration());
        }
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        loadConfig();
        zapController.start();
    }
}
