package br.com.muxe.auth.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.Commands;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import br.com.muxe.core.bukkit.command.framework.registry.CommandRegistrar;
import br.com.muxe.core.bukkit.command.framework.registry.RegisteredCommand;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthCommandsTest {

    private static final ConfigurationSection CONFIG = messagesSection();
    private static final MessageCatalog MESSAGES = MessageCatalog.from(CONFIG);

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
            .register(new AuthCommands(null), new AuthSetupCommand(null, null, null, null, MESSAGES));
    }

    @Test
    void theLoginLabelsAreTheOnesAllowedBeforeLoggingIn() {
        Set<String> labels = new HashSet<>();
        for (String name : List.of(AuthCommands.LOGIN, AuthCommands.REGISTER)) {
            labels.add(name);
            labels.addAll(registered.get(name).definition().aliases());
        }

        assertThat(labels).isEqualTo(AuthCommands.ALLOWED_LABELS);
    }

    @Test
    void wrongArgumentCountsShowTheUsage() {
        Player player = sender(Player.class);

        registered.get(AuthCommands.LOGIN).run(player, "logar", new String[0]);
        registered.get(AuthCommands.LOGIN).run(player, "logar", new String[] {"a", "b"});
        registered.get(AuthCommands.REGISTER).run(player, "registrar", new String[] {"a"});

        assertThat(received).containsExactly(MESSAGES.get("usage-login"), MESSAGES.get("usage-login"), MESSAGES.get("usage-register"));
    }

    @Test
    void onlyPlayersLogIn() {
        registered.get(AuthCommands.LOGIN).run(sender(ConsoleCommandSender.class), "login", new String[] {"senha"});

        assertThat(received).containsExactly(MESSAGES.get("players-only"));
    }

    @Test
    void passwordsAreNeverCompleted() {
        Player player = sender(Player.class);

        assertThat(registered.get(AuthCommands.LOGIN).complete(player, new String[] {""})).isEmpty();
        assertThat(registered.get(AuthCommands.REGISTER).complete(player, new String[] {"abc", ""})).isEmpty();
    }

    @Test
    void theSetupNeedsItsPermission() {
        registered.get(AuthSetupCommand.NAME).run(sender(Player.class), "authsetup", new String[] {"spawn"});

        assertThat(received).containsExactly(MESSAGES.get("no-permission"));
    }

    @Test
    void theConfigurationHasEveryCommandMessage() {
        assertThat(CommandMessages.ALL).allSatisfy(key -> assertThat(CONFIG.isString(key)).as(key).isTrue());
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

    private static ConfigurationSection messagesSection() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(AuthCommandsTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader).getConfigurationSection("messages");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
