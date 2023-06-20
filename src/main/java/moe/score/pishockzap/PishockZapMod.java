package moe.score.pishockzap;

import com.google.gson.Gson;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.PiShockApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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

    private static final KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.pishock-zap.toggle",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_F12,
        "key.category.pishock-zap"
    ));

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

        if (hp == maxHealth) {
            // Player is at full HP, can this really be called damage?
            // (Just in case other mods play with max health, it's not fair to zap the player for that)
            // Note: this return must be after updating player HP in the watcher, otherwise the watcher will
            // report incorrect damage the next time the player takes damage.
            return;
        }

        if (damage > 0) {
            boolean deathZap = hp == 0;
            ShockDistribution distribution = deathZap && config.isShockOnDeath() ? config.getShockDistributionDeath() : config.getShockDistribution();
            int damageEquivalent = config.isShockOnHealth() ? maxHealth - hp : damage;
            if (maxHealth != 20) {
                // Normalize damage to 20 HP if max health is not 20
                // This keeps zap intensity consistent based on percentage of max health
                // And means if you mod yourself to play a super tanky character, you'll get weaker zaps and vice versa
                damageEquivalent = Math.round(damageEquivalent * MAX_DAMAGE / (float) maxHealth);
            }
            if (damageEquivalent > MAX_DAMAGE) {
                logger.warning("Damage equivalent " + damageEquivalent + " exceeds max damage " + MAX_DAMAGE + ", capping");
                damageEquivalent = MAX_DAMAGE;
            }
            logger.info("Death? " + deathZap + ", damage: " + damage + ", hp: " + hp + ", damage equivalent: " + damageEquivalent);
            zapController.queueShock(distribution, deathZap, damageEquivalent, config.getDuration());
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
                    player.sendMessage(Text.translatable(key).fillStyle(color), false);
                }
            }
        });
    }
}
