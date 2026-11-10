package br.com.muxe.afk.bootstrap;

import br.com.muxe.afk.config.AfkServerSettings;
import br.com.muxe.afk.listener.AfkArrivalListener;
import br.com.muxe.afk.listener.AfkChatListener;
import br.com.muxe.afk.listener.AfkMoveListener;
import br.com.muxe.afk.listener.AfkProtectionListener;
import br.com.muxe.afk.route.AfkReturner;
import br.com.muxe.afk.route.ReturnRoute;
import br.com.muxe.afk.session.AfkSessions;
import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.location.ConfigLocation;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.afk.AfkReturns;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AfkBootstrap {

    private final JavaPlugin plugin;

    public AfkBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        MuxeCore core = MuxeCore.get();
        AfkServerSettings settings = AfkServerSettings.from(config);
        MessageCatalog messages = core.messages().merge(MessageCatalog.from(LocalizedConfig.load(plugin), "messages"));

        AfkSessions sessions = new AfkSessions(core.clock(), settings.arrivalGrace(), settings.retryAfter());
        AfkReturner returner = new AfkReturner(sessions, new AfkReturns(core.store()), new ReturnRoute(core.servers(), settings.fallbackGroup()), core.network(), core.async());

        PluginManager plugins = plugin.getServer().getPluginManager();
        plugins.registerEvents(new AfkArrivalListener(plugin.getServer(), new ConfigLocation(plugin, "spawn"), returner, messages), plugin);
        plugins.registerEvents(new AfkMoveListener(returner), plugin);
        plugins.registerEvents(new AfkChatListener(), plugin);
        plugins.registerEvents(new AfkProtectionListener(core.buildMode()), plugin);
    }
}
