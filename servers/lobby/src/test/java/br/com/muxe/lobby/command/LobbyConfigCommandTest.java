package br.com.muxe.lobby.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.config.ConfigCommand;
import br.com.muxe.core.bukkit.command.config.ConfigPathSuggestions;
import br.com.muxe.core.bukkit.command.config.ConfigReplies;
import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.command.framework.registry.CommandRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.RegisteredCommand;
import br.com.muxe.core.bukkit.config.edit.ConfigEditor;
import br.com.muxe.core.bukkit.config.edit.ConfigStore;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.config.live.LiveValue;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.lobby.config.LobbyReloads;
import br.com.muxe.lobby.scoreboard.ScoreboardTemplate;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LobbyConfigCommandTest {

    private static final MessageCatalog MESSAGES = MessageCatalog.of(Map.ofEntries(
        Map.entry("no-permission", "no-permission"),
        Map.entry("players-only", "players-only"),
        Map.entry("config.set", "set {path} {value}"),
        Map.entry("config.live", "live {components}"),
        Map.entry("config.restart", "restart"),
        Map.entry("config.unknown-path", "unknown {path}"),
        Map.entry("config.location-set", "located {path}")
    ));

    private final Map<String, RegisteredCommand> registered = new HashMap<>();
    private final List<String> received = new ArrayList<>();
    private final List<String> events = new ArrayList<>();
    private FileConfiguration config;
    private LiveValue<Localized<ScoreboardTemplate>> scoreboard;

    @BeforeEach
    void register() {
        config = defaultConfig();
        scoreboard = new LiveValue<>(Localized.same(ScoreboardTemplate.from(config)));
        ConfigStore store = new ConfigStore() {
            @Override
            public FileConfiguration config() {
                return config;
            }

            @Override
            public void save() {
                events.add("saved");
            }

            @Override
            public void reload() {
                events.add("reloaded");
            }
        };
        ConfigEditor editor = new ConfigEditor(store, LobbyReloads.create(scoreboard, edited -> LocalizedConfig.standardOnly((Configuration) edited), () -> events.add("spawn")));
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
        Commands commands = new Commands(ArgumentTypes.basic(), new SuggestionRegistry(), MESSAGES, Logger.getLogger("test"), registrar);
        ConfigPathSuggestions.register(commands, editor);
        commands.register(new LobbyConfigCommand(editor, new ConfigReplies(MESSAGES)));
    }

    @Test
    void theCommandIsGuardedByTheConfigPermission() {
        registered.get("lobbyconfig").run(sender(Player.class, false), "lobbyconfig", new String[] {"definir", "scoreboard.title", "x"});

        assertThat(received).containsExactly("no-permission");
        assertThat(config.getString("scoreboard.title")).isEqualTo("&b&lMUXE");
        assertThat(registered.get("lobbyconfig").definition().permission()).contains(ConfigCommand.PERMISSION);
    }

    @Test
    void changingTheScoreboardTitleSwapsTheLiveTemplateWithoutTouchingItsLines() {
        List<String> lines = scoreboard.get().standard().lines();

        registered.get("lobbyconfig").run(sender(Player.class, true), "lobbyconfig", new String[] {"definir", "scoreboard.title", "&aNovo", "titulo"});

        assertThat(scoreboard.get().standard().title()).isEqualTo("&aNovo titulo");
        assertThat(scoreboard.get().standard().lines()).isEqualTo(lines);
        assertThat(received).containsExactly("set scoreboard.title &aNovo titulo", "live scoreboard.title");
    }

    @Test
    void changingAScoreboardLineIsAppliedLive() {
        registered.get("lobbyconfig").run(sender(Player.class, true), "lobbyconfig", new String[] {"adicionar", "scoreboard.lines", "&7Novo"});

        assertThat(scoreboard.get().standard().lines()).endsWith("&7Novo");
    }

    @Test
    void settingsOutsideTheLiveComponentsWaitForARestart() {
        registered.get("lobbyconfig").run(sender(Player.class, true), "lobbyconfig", new String[] {"definir", "arrival.fly-height", "8"});

        assertThat(config.getDouble("arrival.fly-height")).isEqualTo(8.0);
        assertThat(received).containsExactly("set arrival.fly-height 8", "restart");
    }

    @Test
    void theSpawnCanBeStoredWithTheNewFlagAndReloadsTheSpawnService() {
        Player player = located(true);

        registered.get("lobbyconfig").run(player, "lobbyconfig", new String[] {"local", "spawn"});
        registered.get("lobbyconfig").run(player, "lobbyconfig", new String[] {"local", "spawn", "--novo"});

        assertThat(received).containsExactly("unknown spawn", "located spawn", "live spawn");
        assertThat(config.getString("spawn.world")).isEqualTo("lobby");
        assertThat(config.getDouble("spawn.x")).isEqualTo(10.25);
        assertThat(events).containsExactly("saved", "spawn");
    }

    @Test
    void pathsAreCompletedFromTheLobbyConfiguration() {
        assertThat(registered.get("lobbyconfig").complete(sender(Player.class, true), new String[] {"ver", "scoreb"})).containsExactly("scoreboard.");
        assertThat(registered.get("lobbyconfig").complete(sender(Player.class, true), new String[] {"ver", "scoreboard.t"})).containsExactly("scoreboard.title");
    }

    private Player located(boolean allowed) {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] {World.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> "lobby";
            case "hashCode" -> 1;
            case "equals" -> proxy == args[0];
            default -> null;
        });
        Location location = new Location(world, 10.25, 64, -3, 90f, 0f);
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "sendMessage" -> {
                received.add((String) args[0]);
                yield null;
            }
            case "getLocation" -> location;
            case "hasPermission" -> allowed;
            case "getName" -> "Steve";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> method.getReturnType() == boolean.class ? false : null;
        });
    }

    private <T extends CommandSender> T sender(Class<T> type, boolean allowed) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> switch (method.getName()) {
            case "sendMessage" -> {
                received.add((String) args[0]);
                yield null;
            }
            case "hasPermission" -> allowed;
            case "getName" -> "Steve";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> method.getReturnType() == boolean.class ? false : null;
        }));
    }

    private static YamlConfiguration defaultConfig() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(LobbyConfigCommandTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
