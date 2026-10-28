package br.com.muxe.core.bukkit.permission;

import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.sync.PermissionSync;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.plugin.java.JavaPlugin;

public final class PermissionService implements Listener, Runnable {

    public static final long EXPIRY_CHECK_TICKS = 20L;
    public static final Duration LOAD_TIMEOUT = Duration.ofSeconds(5);

    private final Server server;
    private final PermissionSync sync;
    private final PermissionCache cache;
    private final PermissibleInjector injector;
    private final AttachmentPermissions attachments;
    private final Clock clock;
    private final Logger logger;
    private final Map<UUID, PermissibleBase> originals = new ConcurrentHashMap<>();
    private final List<Consumer<Player>> listeners = new CopyOnWriteArrayList<>();

    private PermissionService(JavaPlugin plugin, PermissionSync sync, String serverGroup, Clock clock) {
        this.server = plugin.getServer();
        this.sync = sync;
        this.cache = new PermissionCache(sync::data, serverGroup, clock);
        this.injector = PermissibleInjector.resolve(server, plugin.getLogger());
        this.attachments = new AttachmentPermissions(plugin);
        this.clock = clock;
        this.logger = plugin.getLogger();
    }

    public static PermissionService start(JavaPlugin plugin, PermissionSync sync, String serverGroup, Clock clock) {
        PermissionService service = new PermissionService(plugin, sync, serverGroup, clock);
        sync.onUserChange(service::userChanged);
        sync.onDataChange(service::dataChanged);
        plugin.getServer().getPluginManager().registerEvents(service, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, service, EXPIRY_CHECK_TICKS, EXPIRY_CHECK_TICKS);
        sync.start().thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, service::adoptOnlinePlayers));
        return service;
    }

    public PermissionSync sync() {
        return sync;
    }

    public PermissionData data() {
        return sync.data();
    }

    public Clock clock() {
        return clock;
    }

    public Optional<ResolvedPermissions> resolved(UUID player) {
        return cache.resolved(player);
    }

    public Optional<PermissionUser> user(UUID player) {
        return cache.user(player);
    }

    public void onChange(Consumer<Player> listener) {
        listeners.add(listener);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            sync.load(event.getUniqueId()).get(LOAD_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Could not load the permissions of " + event.getName() + " before login: " + exception);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (cache.user(player.getUniqueId()).isEmpty()) {
            cache.put(sync.cached(player.getUniqueId()).orElse(PermissionUser.empty(player.getUniqueId())), worldOf(player));
        }
        inject(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginResult(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            forget(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        moved(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        moved(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer());
    }

    @Override
    public void run() {
        for (UUID id : cache.refreshExpired()) {
            Player player = server.getPlayer(id);
            if (player != null) {
                changed(player);
            }
        }
    }

    public void stop() {
        for (Player player : server.getOnlinePlayers()) {
            PermissibleBase original = originals.remove(player.getUniqueId());
            if (original != null) {
                injector.restore(player, original);
            }
            attachments.remove(player);
        }
        listeners.clear();
        sync.stop();
    }

    private void adoptOnlinePlayers() {
        for (Player player : server.getOnlinePlayers()) {
            cache.put(PermissionUser.empty(player.getUniqueId()), worldOf(player));
            inject(player);
            sync.load(player.getUniqueId());
        }
    }

    private void inject(Player player) {
        if (originals.containsKey(player.getUniqueId())) {
            return;
        }
        MuxePermissible permissible = new MuxePermissible(player, permission -> cache.check(player.getUniqueId(), permission));
        injector.inject(player, permissible).ifPresent(original -> originals.put(player.getUniqueId(), original));
    }

    private void userChanged(UUID id, PermissionUser user) {
        Player player = server.getPlayer(id);
        String world = player != null ? worldOf(player) : cache.world(id).orElseGet(this::defaultWorld);
        cache.put(user, world);
        if (player != null) {
            changed(player);
        }
    }

    private void dataChanged(PermissionData data) {
        cache.refreshAll();
        server.getOnlinePlayers().forEach(this::changed);
    }

    private void moved(Player player) {
        cache.moveTo(player.getUniqueId(), worldOf(player));
        changed(player);
    }

    private void changed(Player player) {
        if (!originals.containsKey(player.getUniqueId())) {
            cache.resolved(player.getUniqueId()).ifPresent(resolved -> attachments.apply(player, resolved));
        } else {
            player.recalculatePermissions();
        }
        for (Consumer<Player> listener : listeners) {
            try {
                listener.accept(player);
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "A permission listener failed for " + player.getName(), exception);
            }
        }
    }

    private void forget(Player player) {
        UUID id = player.getUniqueId();
        PermissibleBase original = originals.remove(id);
        if (original != null) {
            injector.restore(player, original);
        }
        attachments.forget(id);
        cache.remove(id);
        sync.forget(id);
    }

    private String worldOf(Player player) {
        return player.getWorld() != null ? player.getWorld().getName() : defaultWorld();
    }

    private String defaultWorld() {
        return server.getWorlds().isEmpty() ? "world" : server.getWorlds().getFirst().getName();
    }
}
