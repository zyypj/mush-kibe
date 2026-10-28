package br.com.muxe.core.bukkit.command.framework.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.junit.jupiter.api.Test;

class PermissionMessagesTest {

    private static final String MESSAGE = "§cVocê não possui permissão para usar este comando.";

    private final Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] {Server.class}, (proxy, method, args) -> null);
    private final SimpleCommandMap map = new SimpleCommandMap(server);

    @Test
    void everyCommandOfEveryPluginGetsTheSameMessage() {
        Command other = new Command("outro") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return true;
            }
        };
        other.setPermissionMessage("I'm sorry, but you do not have permission.");
        map.register("outroplugin", other);

        int changed = PermissionMessages.apply(map, MESSAGE);

        assertThat(changed).isPositive();
        assertThat(map.getCommands()).allSatisfy(command -> assertThat(command.getPermissionMessage()).isEqualTo(MESSAGE));
        assertThat(map.getCommand("version").getPermissionMessage()).isEqualTo(MESSAGE);
        assertThat(PermissionMessages.apply(map, MESSAGE)).isZero();
    }

    @Test
    void anUnknownCommandMapIsLeftAlone() {
        CommandMap custom = (CommandMap) Proxy.newProxyInstance(CommandMap.class.getClassLoader(), new Class<?>[] {CommandMap.class},
            (proxy, method, args) -> method.getReturnType().equals(List.class) ? List.of() : null);

        assertThat(PermissionMessages.apply(custom, MESSAGE)).isZero();
    }
}
