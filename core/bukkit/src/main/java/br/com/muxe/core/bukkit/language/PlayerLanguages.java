package br.com.muxe.core.bukkit.language;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.language.application.command.ChangeLanguageCommand;
import br.com.muxe.core.common.language.application.query.FindLanguageQuery;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.messaging.bus.PacketBus;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.language.LanguageChangedPacket;
import br.com.muxe.core.common.result.Result;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLanguages implements LanguageLookup, Listener {

    private static final Duration LOAD_TIMEOUT = Duration.ofSeconds(3);

    private final Map<UUID, Language> languages = new ConcurrentHashMap<>();
    private final List<Consumer<Player>> listeners = new CopyOnWriteArrayList<>();
    private final CommandBus commands;
    private final QueryBus queries;
    private final PacketBus packets;
    private final Executor mainThread;
    private final Logger logger;

    public PlayerLanguages(CommandBus commands, QueryBus queries, PacketBus packets, Executor mainThread, Logger logger) {
        this.commands = commands;
        this.queries = queries;
        this.packets = packets;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    @Override
    public Language of(UUID player) {
        return languages.getOrDefault(player, Language.DEFAULT);
    }

    public Language of(Player player) {
        return of(player.getUniqueId());
    }

    public void onChange(Consumer<Player> listener) {
        listeners.add(listener);
    }

    public CompletableFuture<Result<Void>> change(Player player, Language language) {
        UUID id = player.getUniqueId();
        return commands.dispatch(new ChangeLanguageCommand(id, language)).thenApply(result -> {
            if (result.isSuccess()) {
                packets.publish(PacketTarget.proxy(), new LanguageChangedPacket(id, language)).exceptionally(error -> {
                    logger.log(Level.FINE, "Could not tell the proxy about a language change", error);
                    return null;
                });
                remember(id, language);
            }
            return result;
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        UUID id = event.getUniqueId();
        try {
            languages.put(id, queries.ask(new FindLanguageQuery(id)).get(LOAD_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Could not load the language of " + event.getName() + " before login: " + exception);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginResult(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            languages.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        languages.remove(event.getPlayer().getUniqueId());
    }

    void remember(UUID id, Language language) {
        Language previous = languages.put(id, language);
        if (previous == language) {
            return;
        }
        mainThread.execute(() -> {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                return;
            }
            for (Consumer<Player> listener : listeners) {
                try {
                    listener.accept(player);
                } catch (RuntimeException exception) {
                    logger.log(Level.WARNING, "A language listener failed for " + player.getName(), exception);
                }
            }
        });
    }
}
