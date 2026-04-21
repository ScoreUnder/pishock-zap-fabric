package moe.score.pishockzap.compat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class PlayerCompat {
    public static void displayInChat(LocalPlayer player, Component text) {
        player.displayClientMessage(text, false);
    }
}
