package br.com.muxe.core.bukkit.preference;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.preference.application.command.ChangePreferenceCommand;
import br.com.muxe.core.common.preference.application.query.FindPreferencesQuery;
import br.com.muxe.core.common.preference.domain.Preference;
import br.com.muxe.core.common.preference.domain.Preferences;
import br.com.muxe.core.common.result.Result;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PreferenceService implements Listener {

    private final CommandBus commands;
    private final QueryBus queries;
    private final Logger logger;
    private final Map<UUID, Preferences> online = new ConcurrentHashMap<>();

    public PreferenceService(CommandBus commands, QueryBus queries, Logger logger) {
        this.commands = commands;
        this.queries = queries;
        this.logger = logger;
    }

    public Preferences of(UUID player) {
        return online.getOrDefault(player, Preferences.DEFAULTS);
    }

    public CompletableFuture<Preferences> load(UUID player) {
        Preferences cached = online.get(player);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return queries.ask(new FindPreferencesQuery(player));
    }

    public void change(UUID player, Preference preference, String value) {
        online.compute(player, (id, current) -> (current == null ? Preferences.DEFAULTS : current).with(preference, value));
        commands.dispatch(new ChangePreferenceCommand(player, preference, value)).whenComplete((result, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not save the preference " + preference.key() + " of " + player + ": " + error.getMessage());
            } else if (result instanceof Result.Failure<Void> failure) {
                logger.warning("Could not save the preference " + preference.key() + " of " + player + ": " + failure.problem().message());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        queries.ask(new FindPreferencesQuery(id)).whenComplete((preferences, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not load the preferences of " + event.getPlayer().getName() + ": " + error.getMessage());
                return;
            }
            if (event.getPlayer().isOnline()) {
                online.merge(id, preferences, (loaded, changed) -> changed);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        online.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        event.getRecipients().removeIf(recipient -> !recipient.equals(sender) && !of(recipient.getUniqueId()).on(Preference.CHAT));
    }
}
