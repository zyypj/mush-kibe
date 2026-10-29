package br.com.muxe.core.bukkit.command.framework.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnknownCommandsTest {

    private static final String MESSAGE = "§cComando não encontrado.";

    private final Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] {Server.class},
        (proxy, method, args) -> method.getName().equals("getOnlinePlayers") ? List.of() : null);
    private final SimpleCommandMap map = new SimpleCommandMap(server);
    private final List<String> received = new ArrayList<>();
    private final Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
        if (method.getName().equals("sendMessage")) {
            received.add((String) args[0]);
        }
        if (method.getName().equals("getServer")) {
            return server;
        }
        return method.getReturnType() == boolean.class ? false : null;
    });
    private final UnknownCommands unknown = new UnknownCommands(map, MESSAGE);

    @BeforeEach
    void registerFly() {
        map.register("muxe-lobby", new Command("fly", "", "/fly", List.of("voar")) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return true;
            }
        });
    }

    @Test
    void anUnknownCommandIsCancelledWithTheMessage() {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/naoexiste algo");

        unknown.onCommand(event);

        assertThat(event.isCancelled()).isTrue();
        assertThat(received).containsExactly(MESSAGE);
    }

    @Test
    void knownCommandsAliasesAndPluginPrefixesPassThrough() {
        for (String typed : List.of("/fly", "/FLY", "/voar", "/muxe-lobby:fly agora", "/version")) {
            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, typed);

            unknown.onCommand(event);

            assertThat(event.isCancelled()).as(typed).isFalse();
        }
        assertThat(received).isEmpty();
    }

    @Test
    void aBareSlashIsUnknown() {
        assertThat(unknown.known("/")).isFalse();
        assertThat(unknown.known("/ fly")).isFalse();
        assertThat(UnknownCommands.label("/fly on")).isEqualTo("fly");
    }
}
