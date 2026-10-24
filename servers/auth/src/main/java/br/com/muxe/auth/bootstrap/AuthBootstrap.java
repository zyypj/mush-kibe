package br.com.muxe.auth.bootstrap;

import br.com.muxe.auth.challenge.ChallengeCodes;
import br.com.muxe.auth.challenge.ChallengeRenderer;
import br.com.muxe.auth.challenge.ChallengeTracker;
import br.com.muxe.auth.challenge.HologramChallengeScreen;
import br.com.muxe.auth.command.AuthCommands;
import br.com.muxe.auth.command.AuthSetupCommand;
import br.com.muxe.auth.config.AuthSettings;
import br.com.muxe.auth.credential.CredentialModule;
import br.com.muxe.auth.credential.crypto.Pbkdf2PasswordHasher;
import br.com.muxe.auth.credential.infrastructure.postgres.PostgresCredentialRepository;
import br.com.muxe.auth.flow.AuthFlow;
import br.com.muxe.auth.flow.AuthPrompts;
import br.com.muxe.auth.flow.AuthReminder;
import br.com.muxe.auth.flow.ProxyAuthenticationCheck;
import br.com.muxe.auth.listener.AuthPlayerListener;
import br.com.muxe.auth.listener.AuthRestrictionListener;
import br.com.muxe.auth.listener.ChallengeChatListener;
import br.com.muxe.auth.session.AuthSessionRegistry;
import br.com.muxe.auth.session.LoginAttemptLimiter;
import br.com.muxe.auth.setup.AuthLocations;
import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.buildmode.BuildModeHook;
import br.com.muxe.core.bukkit.config.BukkitConfigSource;
import br.com.muxe.core.bukkit.hologram.PixelHolograms;
import br.com.muxe.core.bukkit.location.ConfigLocation;
import br.com.muxe.core.bukkit.nms.PacketHologramTransport;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import java.security.SecureRandom;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class AuthBootstrap {

    public static final String MODULE = "auth";

    private static final long TICKS_PER_SECOND = 20L;

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

        SecureRandom random = new SecureRandom();
        CredentialModule.register(
            core.commands(),
            core.queries(),
            new PostgresCredentialRepository(),
            new Pbkdf2PasswordHasher(settings.hashIterations(), random),
            settings.passwordPolicy(),
            core.transactions(),
            core.clock()
        );

        AuthSettings.ChallengeSettings challenge = settings.challenge();
        AuthSessionRegistry sessions = new AuthSessionRegistry();
        ChallengeTracker tracker = new ChallengeTracker(new ChallengeCodes(random, challenge.length()), challenge.maxAttempts());
        PixelHolograms codeText = PixelHolograms.standard();
        if (!PacketHologramTransport.isAvailable()) {
            plugin.getLogger().warning("Packet holograms are unavailable, the challenge code will not be drawn");
        }
        ChallengeRenderer renderer = new ChallengeRenderer(tracker, new HologramChallengeScreen(codeText));
        long reminderSeconds = Math.max(1, settings.reminderInterval().getSeconds());
        AuthPrompts prompts = new AuthPrompts(messages, (int) ((reminderSeconds + 1) * TICKS_PER_SECOND));
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(core.clock(), settings.maxAttempts(), settings.attemptWindow());
        ProxyAuthenticationCheck proxyCheck = new ProxyAuthenticationCheck(core.packets(), core.requestTimeout());
        AuthFlow flow = new AuthFlow(core, proxyCheck, sessions, limiter, tracker, renderer, challenge.enabled(), prompts, messages, plugin.getLogger());
        AuthLocations locations = new AuthLocations(
            new ConfigLocation(plugin, "locations.spawn"),
            new ConfigLocation(plugin, "locations.challenge-code"),
            challenge.distance()
        );

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new AuthPlayerListener(plugin, flow, messages, settings.loginTimeout(), locations, codeText.height()), plugin);
        core.buildMode().addHook(new BuildModeHook() {
            @Override
            public boolean allows(Player player) {
                return sessions.isAuthenticated(player.getUniqueId());
            }
        });
        plugins.registerEvents(new AuthRestrictionListener(sessions, locations, settings.voidLevel(), messages, core.buildMode()), plugin);
        plugins.registerEvents(new ChallengeChatListener(flow, core.mainThread()), plugin);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskTimer(plugin, renderer, ChallengeRenderer.PERIOD_TICKS, ChallengeRenderer.PERIOD_TICKS);
        scheduler.runTaskTimer(plugin, new AuthReminder(sessions, prompts, reminderSeconds), AuthReminder.PERIOD_TICKS, AuthReminder.PERIOD_TICKS);

        AuthCommands commands = new AuthCommands(flow, messages);
        bind(AuthCommands.LOGIN, commands);
        bind(AuthCommands.REGISTER, commands);
        bind(AuthSetupCommand.NAME, new AuthSetupCommand(plugin, flow, locations, codeText, messages));
    }

    private void bind(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command /" + name + " is missing from plugin.yml");
        }
        command.setExecutor(executor);
    }
}
