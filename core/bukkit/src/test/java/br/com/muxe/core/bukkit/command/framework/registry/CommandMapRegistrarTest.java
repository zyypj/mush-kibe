package br.com.muxe.core.bukkit.command.framework.registry;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.command.framework.TestCommands;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class CommandMapRegistrarTest {

    private final Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] {Server.class}, (proxy, method, args) -> null);
    private final Plugin plugin = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class},
        (proxy, method, args) -> method.getName().equals("getName") ? "Muxe-Test" : null);
    private final SimpleCommandMap map = new SimpleCommandMap(server);
    private final Commands commands = new Commands(ArgumentTypes.basic(), new SuggestionRegistry(), TestCommands.MESSAGES, Logger.getLogger("test"),
        new CommandMapRegistrar(plugin, map, Logger.getLogger("test")));

    @Test
    void commandsAreRegisteredUnderTheirNameAliasesAndPluginPrefix() {
        Spawn spawn = new Spawn();
        commands.register(spawn);
        FakeSender staff = FakeSender.named("Staff", "test.spawn");

        map.getCommand("s").execute(staff.player(), "s", new String[] {"definir"});

        assertThat(map.getCommand("spawn")).isSameAs(map.getCommand("muxe-test:spawn"));
        assertThat(map.getCommand("spawn").getPermission()).isEqualTo("test.spawn");
        assertThat(map.getCommand("spawn")).isInstanceOf(DynamicCommand.class);
        assertThat(((DynamicCommand) map.getCommand("spawn")).getPlugin()).isSameAs(plugin);
        assertThat(map.getCommand("spawn").tabComplete(staff.player(), "spawn", new String[] {"d"})).containsExactly("definir");
        assertThat(spawn.calls).containsExactly("set");
    }

    @Test
    void unregisteringRemovesEveryLabelOfTheCommand() {
        commands.register(new Spawn());

        commands.unregisterAll();

        assertThat(map.getCommand("spawn")).isNull();
        assertThat(map.getCommand("s")).isNull();
        assertThat(map.getCommand("muxe-test:spawn")).isNull();
        assertThat(map.getCommand("version")).isNotNull();
    }

    @Test
    void aTakenNameKeepsThePrefixedLabel() {
        commands.register(new Spawn());
        Commands other = new Commands(ArgumentTypes.basic(), new SuggestionRegistry(), TestCommands.MESSAGES, Logger.getLogger("test"),
            new CommandMapRegistrar(plugin, map, Logger.getLogger("test")));
        Spawn second = new Spawn();

        other.register(second);
        map.getCommand("muxe-test:spawn").execute(FakeSender.named("Staff", "test.spawn").player(), "spawn", new String[0]);

        assertThat(map.getCommand("spawn")).isNotSameAs(map.getCommand("muxe-test:spawn"));
        assertThat(second.calls).containsExactly("go");
    }

    @Test
    void theCommandMapIsNotFoundOnAServerWithoutIt() {
        assertThat(CommandMapRegistrar.commandMap(server)).isEmpty();
    }

    @Command(name = "spawn", aliases = "s")
    @Permission("test.spawn")
    public static final class Spawn {

        final List<String> calls = new ArrayList<>();

        @Default
        public void go(CommandSender sender) {
            calls.add("go");
        }

        @Subcommand("definir")
        public void set(CommandSender sender) {
            calls.add("set");
        }
    }
}
