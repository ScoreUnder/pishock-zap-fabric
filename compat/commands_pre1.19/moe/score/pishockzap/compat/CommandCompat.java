package moe.score.pishockzap.compat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;

import java.util.function.Consumer;

@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class CommandCompat {
    public static void whenRegistering(Consumer<CommandDispatcher<FabricClientCommandSource>> action) {
        action.accept(ClientCommandManager.DISPATCHER);
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommandManager.literal(name);
    }
}
