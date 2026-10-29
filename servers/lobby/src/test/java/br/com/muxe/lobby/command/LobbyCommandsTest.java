package br.com.muxe.lobby.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.command.framework.registry.CommandRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.RegisteredCommand;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LobbyCommandsTest {

    private static final MessageCatalog MESSAGES = MessageCatalog.of(Map.of(
        "players-only", "players-only",
        "no-permission", "no-permission",
        "play-usage", "usage {games}",
        "play-unknown", "unknown {games}",
        "fly-enabled", "fly on",
        "fly-disabled", "fly off"
    ));

    private final Map<String, RegisteredCommand> registered = new HashMap<>();
    private final List<String> received = new ArrayList<>();

    @BeforeEach
    void register() {
        CommandRegistrar registrar = new CommandRegistrar() {
            @Override
            public void register(RegisteredCommand command) {
                registered.put(command.definition().name(), command);
            }

            @Override
            public void unregisterAll() {
                registered.clear();
            }
        };
        new Commands(ArgumentTypes.basic(), new SuggestionRegistry(), MESSAGES, Logger.getLogger("test"), registrar)
            .register(new PlayCommand(null, MESSAGES, List.of("skywars", "bedwars")), new SetSpawnCommand(null, MESSAGES),
                new FlyCommand(MESSAGES));
    }

    @Test
    void playListsTheGamesWhenTheModeIsMissingOrUnknown() {
        Player player = sender(Player.class);

        registered.get("jogar").run(player, "jogar", new String[0]);
        registered.get("jogar").run(player, "play", new String[] {"skywars", "solo"});
        registered.get("jogar").run(player, "jogar", new String[] {"pvp"});

        assertThat(received).containsExactly("usage skywars, bedwars", "usage skywars, bedwars", "unknown skywars, bedwars");
    }

    @Test
    void playCompletesTheConfiguredGames() {
        assertThat(registered.get("jogar").complete(sender(Player.class), new String[] {""})).containsExactly("bedwars", "skywars");
        assertThat(registered.get("jogar").complete(sender(Player.class), new String[] {"s"})).containsExactly("skywars");
    }

    @Test
    void onlyPlayersPlayAndOnlyStaffSetTheSpawn() {
        registered.get("jogar").run(sender(ConsoleCommandSender.class), "jogar", new String[] {"skywars"});
        registered.get("setspawn").run(sender(Player.class), "setspawn", new String[0]);

        assertThat(received).containsExactly("players-only", "no-permission");
        assertThat(registered.get("jogar").definition().aliases()).containsExactly("play");
    }

    @Test
    void flyTurnsFlightOnAndOffAndLandsThePlayer() {
        Map<String, Boolean> state = new HashMap<>(Map.of("allowFlight", false, "flying", false));
        Player player = flyer(state, true);

        registered.get("fly").run(player, "fly", new String[0]);
        assertThat(state).containsEntry("allowFlight", true);
        state.put("flying", true);
        registered.get("fly").run(player, "voar", new String[0]);

        assertThat(state).containsEntry("allowFlight", false).containsEntry("flying", false);
        assertThat(received).containsExactly("fly on", "fly off");
        assertThat(registered.get("fly").definition().aliases()).containsExactly("voar");
    }

    @Test
    void flyNeedsTheLobbyFlyPermission() {
        Map<String, Boolean> state = new HashMap<>(Map.of("allowFlight", false, "flying", false));

        registered.get("fly").run(flyer(state, false), "fly", new String[0]);

        assertThat(state).containsEntry("allowFlight", false);
        assertThat(received).containsExactly("no-permission");
    }

    private Player flyer(Map<String, Boolean> state, boolean allowed) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "sendMessage" -> {
                received.add((String) args[0]);
                yield null;
            }
            case "hasPermission" -> allowed;
            case "getAllowFlight" -> state.get("allowFlight");
            case "isFlying" -> state.get("flying");
            case "setAllowFlight" -> {
                state.put("allowFlight", (Boolean) args[0]);
                yield null;
            }
            case "setFlying" -> {
                state.put("flying", (Boolean) args[0]);
                yield null;
            }
            case "getName" -> "Steve";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> method.getReturnType() == boolean.class ? false : null;
        });
    }

    private <T extends CommandSender> T sender(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> switch (method.getName()) {
            case "sendMessage" -> {
                received.add((String) args[0]);
                yield null;
            }
            case "getName" -> "Steve";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> method.getReturnType() == boolean.class ? false : null;
        }));
    }
}
