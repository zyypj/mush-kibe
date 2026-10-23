package br.com.muxe.auth.bootstrap;

import br.com.muxe.auth.command.AuthCommands;
import br.com.muxe.auth.config.AuthSettings;
import br.com.muxe.auth.credential.CredentialModule;
import br.com.muxe.auth.credential.crypto.Pbkdf2PasswordHasher;
import br.com.muxe.auth.credential.infrastructure.postgres.PostgresCredentialRepository;
import br.com.muxe.auth.flow.AuthFlow;
import br.com.muxe.auth.listener.AuthPlayerListener;
import br.com.muxe.auth.listener.AuthRestrictionListener;
import br.com.muxe.auth.session.AuthSessionRegistry;
import br.com.muxe.auth.session.LoginAttemptLimiter;
import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import java.security.SecureRandom;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuthBootstrap {

    public static final String MODULE = "auth";

    private final JavaPlugin plugin;

    public AuthBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        AuthSettings settings = AuthSettings.from(new BukkitConfigSource(config));
        MessageCatalog messages = MessageCatalog.from(config.getConfigurationSection("messages"));
        MuxeCore core = MuxeCore.get();

        MigrationReport migration = core.migrations().migrate(MODULE, plugin.getClass().getClassLoader());
        plugin.getLogger().info("Schema 'auth' at version " + migration.schemaVersion());

        CredentialModule.register(
            core.commands(),
            core.queries(),
            new PostgresCredentialRepository(),
            new Pbkdf2PasswordHasher(settings.hashIterations(), new SecureRandom()),
            settings.passwordPolicy(),
            core.transactions(),
            core.clock()
        );

        LoginAttemptLimiter limiter = new LoginAttemptLimiter(core.clock(), settings.maxAttempts(), settings.attemptWindow());
        AuthFlow flow = new AuthFlow(core, new AuthSessionRegistry(), limiter, messages, plugin.getLogger());

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new AuthPlayerListener(plugin, flow, messages, settings.loginTimeout()), plugin);
        plugins.registerEvents(new AuthRestrictionListener(messages), plugin);

        AuthCommands commands = new AuthCommands(flow, messages);
        bind(AuthCommands.LOGIN, commands);
        bind(AuthCommands.REGISTER, commands);
    }

    private void bind(String name, AuthCommands executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command /" + name + " is missing from plugin.yml");
        }
        command.setExecutor(executor);
    }
}
