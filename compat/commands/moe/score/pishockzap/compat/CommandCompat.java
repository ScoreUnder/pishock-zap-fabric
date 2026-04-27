package moe.score.pishockzap.compat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.function.Consumer;

@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class CommandCompat {
    public static void whenRegistering(Consumer<CommandDispatcher<FabricClientCommandSource>> action) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            action.accept(dispatcher));
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return ClientCommands.literal(name);
    }
}
