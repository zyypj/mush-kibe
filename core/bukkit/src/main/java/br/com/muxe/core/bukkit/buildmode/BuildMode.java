package br.com.muxe.core.bukkit.buildmode;

import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.buildmode.BuildState;
import br.com.muxe.core.common.server.ServerKind;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BuildMode implements Listener, Runnable {

    public static final String PERMISSION = "muxe.build";
    public static final long ACTION_BAR_PERIOD_TICKS = 20L;

    private final Server server;
    private final BuildState state;
    private final BuildSettings settings;
    private final ServerKind kind;
    private final Executor async;
    private final Executor mainThread;
    private final MessageCatalog messages;
    private final Logger logger;
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final List<BuildModeHook> hooks = new CopyOnWriteArrayList<>();

    public BuildMode(
        Server server,
        BuildState state,
        BuildSettings settings,
        ServerKind kind,
        Executor async,
        Executor mainThread,
        MessageCatalog messages,
        Logger logger
    ) {
        this.server = server;
        this.state = state;
        this.settings = settings;
        this.kind = kind;
        this.async = async;
        this.mainThread = mainThread;
        this.messages = messages;
        this.logger = logger;
    }

    public void addHook(BuildModeHook hook) {
        hooks.add(hook);
    }

    public boolean isBuilding(Player player) {
        return active.contains(player.getUniqueId());
    }

    public Change enter(Player player) {
        if (isBuilding(player)) {
            return Change.UNCHANGED;
        }
        if (!allowed(player)) {
            return Change.NOT_ALLOWED;
        }
        apply(player);
        remember(player, true);
        return Change.CHANGED;
    }

    public Change leave(Player player) {
        if (!active.remove(player.getUniqueId())) {
            return Change.UNCHANGED;
        }
        player.getInventory().clear();
        player.setGameMode(settings.restingMode(kind, server.getDefaultGameMode()));
        ActionBar.send(player, "");
        hooks.forEach(hook -> hook.left(player));
        remember(player, false);
        return Change.CHANGED;
    }

    @Override
    public void run() {
        String text = messages.get("build-action-bar");
        for (UUID builder : Set.copyOf(active)) {
            Player player = server.getPlayer(builder);
            if (player == null) {
                active.remove(builder);
            } else {
                ActionBar.send(player, text);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CompletableFuture.supplyAsync(() -> state.isOn(player.getUniqueId()), async).whenComplete((on, error) -> mainThread.execute(() -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not read the build mode of " + player.getName() + ": " + error.getMessage());
                return;
            }
            if (!on || !player.isOnline() || isBuilding(player)) {
                return;
            }
            if (!player.hasPermission(PERMISSION)) {
                remember(player, false);
                return;
            }
            if (allowed(player)) {
                apply(player);
                remember(player, true);
            }
        }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (settings.adventureOnly(kind) && !BuildRules.allowsGameMode(event.getNewGameMode(), isBuilding(event.getPlayer()))) {
            event.setCancelled(true);
        }
    }

    private boolean allowed(Player player) {
        return hooks.stream().allMatch(hook -> hook.allows(player));
    }

    private void apply(Player player) {
        active.add(player.getUniqueId());
        player.getInventory().clear();
        player.setGameMode(GameMode.CREATIVE);
        ActionBar.send(player, messages.get("build-action-bar"));
        hooks.forEach(hook -> hook.entered(player));
    }

    private void remember(Player player, boolean on) {
        UUID playerId = player.getUniqueId();
        CompletableFuture.runAsync(() -> {
            if (on) {
                state.turnOn(playerId);
            } else {
                state.turnOff(playerId);
            }
        }, async).exceptionally(error -> {
            logger.log(Level.WARNING, "Could not save the build mode of " + player.getName() + ": " + error.getMessage());
            return null;
        });
    }

    public enum Change {
        CHANGED,
        UNCHANGED,
        NOT_ALLOWED
    }
}
