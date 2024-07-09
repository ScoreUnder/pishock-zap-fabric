package moe.score.pishockzap;

import com.google.gson.Gson;
import lombok.Getter;
import moe.score.pishockzap.compat.Translation;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.PiShockSerialApi;
import moe.score.pishockzap.pishockapi.PiShockWebApiV1;
import moe.score.pishockzap.pishockapi.WebHookApi;
import moe.score.pishockzap.shockcalculation.ZapController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class PishockZapMod implements ClientModInitializer {
    public static final int MAX_DAMAGE = 20;
    public static final String NAME = "PiShock-Zap";
    @Getter
    private static PishockZapMod instance = null;

    private static final KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.pishock-zap.toggle",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_F12,
        "key.category.pishock-zap"
    ));

    private final Logger logger = Logger.getLogger(NAME);
    private final Path configFile = FabricLoader.getInstance().getConfigDir().resolve(NAME.toLowerCase() + ".json");
    @Getter
    private final PishockZapConfig config = new PishockZapConfig();
    private final PlayerHpWatcher playerHpWatcher = new PlayerHpWatcher();
    private final ExecutorService apiExecutor = Executors.newSingleThreadExecutor();
    private final ZapController zapController = new ZapController(new PiShockWebApiV1(config, apiExecutor), config);

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

        applyConfigChanges();
    }

    private void applyConfigChanges() {
        // Things that can't be just pulled out of the config object on the fly
        // (because we're too classy to "require restart" for everything)
        // Also getting observers on these properties is a bit of a pain

        // PiShock API type
        // (And serial API port, if we're using the serial API)
        switch (config.getApiType()) {
            case WEB_V1:
                if (!(zapController.getApi() instanceof PiShockWebApiV1)) {
                    zapController.setApi(new PiShockWebApiV1(config, apiExecutor));
                }
                break;
            case SERIAL:
                if (!(zapController.getApi() instanceof PiShockSerialApi piShockSerialApi)
                    || !Objects.equals(piShockSerialApi.getPortName(), config.getSerialPort())) {
                    zapController.setApi(new PiShockSerialApi(config, apiExecutor, config.getSerialPort()));
                }
                break;
            case WEBHOOK:
                if (!(zapController.getApi() instanceof WebHookApi)) {
                    zapController.setApi(new WebHookApi(config, apiExecutor));
                }
                break;
        }
    }

    @SuppressWarnings("unchecked")  // Type erasure means we can't get a Map<String, Object> "safely"
    public void loadConfig() {
        if (!Files.exists(configFile)) {
            logger.info("Config file not found, using default config");
            saveConfig();
            return;
        }

        Gson gson = new Gson();
        Map<String, Object> configMap;
        try {
            configMap = gson.fromJson(Files.newBufferedReader(configFile), Map.class);
        } catch (Exception e) {
            logger.warning("Failed to load config file, exception details follow");
            e.printStackTrace();
            return;
        }

        config.setFromConfig(configMap);

        applyConfigChanges();
    }

    public void onPlayerHpChange() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.isSpectator() || player.isCreative()) {
            // Don't zap spectators or creative players
            // Also, player really shouldn't be null here as it's being called while the player is ticked,
            // but better to be safe than crashy.
            playerHpWatcher.resetPlayer();
            return;
        }

        // HP is a float, and the game uses ceil() when displaying it.
        // Death is when HP <= 0.0, so if the HP is 0.000001, the player
        // is still alive so rounding that down is not appropriate.
        int hp = (int) Math.ceil(player.getHealth());
        int maxHealth = (int) Math.ceil(player.getMaxHealth());
        hp = Math.max(0, Math.min(hp, maxHealth));

        int damage = playerHpWatcher.updatePlayerHpAndGetDamage(player, hp);

        if (hp == maxHealth || maxHealth <= 0) {
            // Player is at full HP, can this really be called damage?
            // (Just in case other mods play with max health, it's not fair to zap the player for that)
            // Note: this return must be after updating player HP in the watcher, otherwise the watcher will
            // report incorrect damage the next time the player takes damage.
            return;
        }

        if (damage > 0) {
            boolean deathZap = hp == 0;
            ShockDistribution distribution = deathZap && config.isShockOnDeath() ? config.getShockDistributionDeath() : config.getShockDistribution();
            float damageEquivalent = config.isShockOnHealth() ? maxHealth - hp : damage;
            damageEquivalent /= maxHealth;
            if (damageEquivalent > 1.0f) {
                logger.warning("Damage equivalent " + damageEquivalent + " exceeds 100% damage, capping");
                damageEquivalent = 1.0f;
            }
            logger.info("Death? " + deathZap + ", damage: " + damage + ", hp: " + hp + ", damage equivalent: " + damageEquivalent);
            zapController.queueShock(distribution, deathZap, damageEquivalent);
        }
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        loadConfig();
        zapController.start();

        registerToggleHotkey();
    }

    private void registerToggleHotkey() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                config.setEnabled(!config.isEnabled());
                saveConfig();

                var player = client.player;
                if (player != null) {
                    Style color = Style.EMPTY.withColor(config.isEnabled() ? 0x00FF00 : 0xFF0000);
                    String key = "message.pishock-zap.toggle." + (config.isEnabled() ? "on" : "off");
                    player.sendMessage(Translation.of(key).fillStyle(color), false);
                }
            }
        });
    }
}
