package br.com.muxe.core.bukkit.command.framework;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import br.com.muxe.core.bukkit.command.framework.registry.UnknownCommands;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.Test;

class LocalizedCommandRepliesTest {

    private static final UUID ENGLISH = UUID.nameUUIDFromBytes("Steve".getBytes(StandardCharsets.UTF_8));
    private static final MessageCatalog MESSAGES = MessageCatalog.of(Map.of(
        Language.PORTUGUESE, Map.of(
            CommandMessages.NO_PERMISSION, "sem permissão",
            CommandMessages.USAGE, "uso {usage}",
            CommandMessages.INVALID_NUMBER, "número inválido {input}",
            UnknownCommands.UNKNOWN_COMMAND, "comando desconhecido"
        ),
        Language.ENGLISH, Map.of(
            CommandMessages.NO_PERMISSION, "no permission",
            CommandMessages.USAGE, "usage {usage}",
            CommandMessages.INVALID_NUMBER, "invalid number {input}",
            UnknownCommands.UNKNOWN_COMMAND, "unknown command"
        )
    ), player -> player.equals(ENGLISH) ? Language.ENGLISH : Language.PORTUGUESE);

    @Test
    void theFrameworkRepliesInTheLanguageOfTheSender() {
        TestCommands commands = new TestCommands(ArgumentTypes.basic(), MESSAGES).register(new SampleCommands.Warp(), new SampleCommands.Help());
        FakeSender english = FakeSender.named("Steve", SampleCommands.WARP);
        FakeSender portuguese = FakeSender.named("Ana", SampleCommands.WARP);
        FakeSender stranger = FakeSender.named("Steve");

        commands.run(english.player(), "warp criar");
        commands.run(english.player(), "ajuda ver dez");
        commands.run(portuguese.player(), "ajuda ver dez");
        commands.run(stranger.player(), "warp");

        assertThat(english.messages()).containsExactly("usage /warp criar <nome> [custo]", "invalid number dez");
        assertThat(portuguese.messages()).containsExactly("número inválido dez");
        assertThat(stranger.messages()).containsExactly("no permission");
    }

    private static Player player(Server server, String name, Set<String> permissions, List<String> received) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getName" -> name;
            case "getUniqueId" -> UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
            case "hasPermission" -> args[0] instanceof String permission && permissions.contains(permission);
            case "sendMessage" -> {
                received.add((String) args[0]);
                yield null;
            }
            default -> method.getReturnType() == boolean.class ? false : null;
        });
    }

    @Test
    void unknownAndForbiddenCommandsAreAnsweredInTheLanguageOfThePlayer() {
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] {Server.class},
            (proxy, method, args) -> method.getName().equals("getOnlinePlayers") ? List.of() : null);
        SimpleCommandMap map = new SimpleCommandMap(server);
        Command fly = new Command("fly") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return true;
            }
        };
        fly.setPermission("muxe.fly");
        map.register("muxe", fly);
        UnknownCommands replies = new UnknownCommands(map, MESSAGES);
        List<String> english = new ArrayList<>();
        List<String> portuguese = new ArrayList<>();
        Player steve = player(server, "Steve", Set.of(), english);
        Player ana = player(server, "Ana", Set.of("muxe.fly"), portuguese);

        PlayerCommandPreprocessEvent unknown = new PlayerCommandPreprocessEvent(steve, "/nada");
        PlayerCommandPreprocessEvent forbidden = new PlayerCommandPreprocessEvent(steve, "/fly");
        PlayerCommandPreprocessEvent allowed = new PlayerCommandPreprocessEvent(ana, "/fly");
        replies.onCommand(unknown);
        replies.onCommand(forbidden);
        replies.onCommand(allowed);

        assertThat(unknown.isCancelled()).isTrue();
        assertThat(forbidden.isCancelled()).isTrue();
        assertThat(allowed.isCancelled()).isFalse();
        assertThat(english).containsExactly("unknown command", "no permission");
        assertThat(portuguese).isEmpty();
    }
}
