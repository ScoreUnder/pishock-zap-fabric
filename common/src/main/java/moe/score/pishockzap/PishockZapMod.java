package moe.score.pishockzap;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.ExtensionMethod;
import lombok.extern.slf4j.Slf4j;
import moe.score.pishockzap.backend.ShockBackendRegistry;
import moe.score.pishockzap.backend.impls.NullBackend;
import moe.score.pishockzap.compat.*;
import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.frontend.ZapController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
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
import java.util.logging.Level;

import static moe.score.pishockzap.util.Gsons.gson;

@ApiStatus.Internal
@ExtensionMethod(TextStyle.class)
@Slf4j(topic = Constants.NAME)
public class PishockZapMod implements ClientModInitializer {
    @Getter
    private static @Nullable PishockZapMod instance = null;

    private static final KeyMapping keyBinding = KeyBindingCompat.registerKeyBinding(
        "key.pishock-zap.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F12,
        "general"
    );

    private final Path configFile = FabricLoader.getInstance().getConfigDir().resolve(Constants.NAME.toLowerCase() + ".json");
    @Getter
    private final PishockZapConfig config = new PishockZapConfig();
    private final PlayerHpWatcher<LocalPlayer> playerHpWatcher = new PlayerHpWatcher<>();
    private final ExecutorService apiExecutor = Executors.newCachedThreadPool();
    @Getter(AccessLevel.PACKAGE)
    private final ZapController zapController = new ZapController(new NullBackend(), config);
    private String currentBackendId = null;

    public void saveConfig() {
        Map<String, Object> configMap = new HashMap<>();
        config.copyToConfig(configMap);

        try (BufferedWriter configWriter = Files.newBufferedWriter(configFile)) {
            gson.toJson(configMap, configWriter);
        } catch (IOException e) {
            log.warn("Failed to save config file, exception details follow", e);
        }

        applyConfigChanges();
    }

    private void applyConfigChanges() {
        // Things that can't be just pulled out of the config object on the fly
        // (because we're too classy to "require restart" for everything)
        // Also getting observers on these properties is a bit of a pain

        // Respawn backend if we are using a different one
        var newBackendId = config.getApiType();
        if (!Objects.equals(currentBackendId, newBackendId)) {
            try {
                zapController.setBackend(ShockBackendRegistry.getCreateFunc(newBackendId).apply(config, apiExecutor));
                currentBackendId = newBackendId;
            } catch (Exception | LinkageError e) {
                log.error("Failed to create shock backend of type \"{}\"", newBackendId, e);
                zapController.setBackend(new NullBackend());
                currentBackendId = null;
            }
        }

        // Update HP watcher, because rounding behavior might have changed
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            PlayerHp hpInfo = getPlayerHp(player);
            playerHpWatcher.updatePlayerHpBypassIgnore(hpInfo.hp());
        }
    }

    @SuppressWarnings("unchecked")  // Type erasure means we can't get a Map<String, Object> "safely"
    public void loadConfig() {
        if (!Files.exists(configFile)) {
            log.info("Config file not found, using default config");
            saveConfig();
            return;
        }

        Map<String, Object> configMap;
        try {
            configMap = gson.fromJson(Files.newBufferedReader(configFile), Map.class);
        } catch (Exception e) {
            log.warn("Failed to load config file, exception details follow", e);
            return;
        }

        config.setFromConfig(configMap);

        applyConfigChanges();
    }

    public void onPlayerHpChange(LocalPlayer player) {
        PlayerHp hpInfo = getPlayerHp(player);
        float damage = playerHpWatcher.updatePlayerHpAndGetDamage(player, hpInfo.hp());

        if (player.isSpectator() || player.isCreative()) {
            // Don't zap spectators or creative players
            return;
        }

        zapController.queueShockForDamage(hpInfo.hp(), hpInfo.maxHealth(), damage);
    }

    private @NonNull PlayerHp getPlayerHp(LocalPlayer player) {
        float hp = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (!config.isFractionalDamage()) {
            // HP is a float, and the game uses ceil() when displaying it.
            // Death is when HP <= 0.0, so if the HP is 0.000001, the player
            // is still alive so rounding that down is not appropriate.
            hp = (float) Math.ceil(hp);
            maxHealth = (float) Math.ceil(maxHealth);
        }
        hp = Math.max(0, Math.min(hp, maxHealth));
        return new PlayerHp(hp, maxHealth);
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        DefaultShockBackends.registerAll();
        loadConfig();
        zapController.start();

        registerClientCommands();
        registerToggleHotkey();
        registerBackendJoinWorldNotifier();
    }

    private void registerToggleHotkey() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.consumeClick()) {
                toggleEnabled(client);
            }
        });
    }

    private void toggleEnabled(Minecraft client) {
        setEnabled(client, !config.isEnabled());
    }

    private void setEnabled(Minecraft client, boolean newEnabled) {
        config.setEnabled(newEnabled);
        saveConfig();

        var player = client.player;
        if (player != null) {
            Style color = Style.EMPTY.withColor(config.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED);
            String key = "message.pishock-zap.toggle." + (config.isEnabled() ? "on" : "off");
            PlayerCompat.displayInChat(player, Translation.of(key).setStyle(color));
            if (currentBackendId == null) {
                var openConfigLink = Translation.of("message.pishock-zap.open_config")
                    .setStyle(Style.EMPTY
                        .withColor(ChatFormatting.RESET)
                        .withCommandOnClick("/" + Constants.ID + " config")
                        .withHoverText(Translation.of("tooltip.pishock-zap.open_config")));
                PlayerCompat.displayInChat(player,
                    Translation.of("message.pishock-zap.backend.not_configured", openConfigLink)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            }
        }
    }

    private void registerClientCommands() {
        CommandCompat.whenRegistering(dispatcher -> {
            dispatcher.register(CommandCompat.literal(Constants.ID)
                .then(CommandCompat.literal("config")
                    .executes(context -> {
                        var minecraft = context.getSource().getClient();
                        minecraft.execute(() -> {
                            var screen = PishockZapModConfigMenu.createConfigScreen(minecraft.screen);
                            minecraft.setScreen(screen);
                        });
                        return 1;
                    }))
                .then(CommandCompat.literal("toggle")
                    .executes(context -> {
                        toggleEnabled(context.getSource().getClient());
                        return 1;
                    }))
                .then(CommandCompat.literal("on")
                    .executes(context -> {
                        setEnabled(context.getSource().getClient(), true);
                        return 1;
                    }))
                .then(CommandCompat.literal("off")
                    .executes(context -> {
                        setEnabled(context.getSource().getClient(), false);
                        return 1;
                    })));
        });
    }

    private void registerBackendJoinWorldNotifier() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> zapController.getBackend().onWorldJoin());
    }

    public static String getVersion() {
        return FabricLoader.getInstance().getModContainer(Constants.ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    private record PlayerHp(float hp, float maxHealth) {
    }
}
