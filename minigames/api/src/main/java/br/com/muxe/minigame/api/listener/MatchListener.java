package br.com.muxe.minigame.api.listener;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.menu.MenuHolder;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.runtime.MatchRules;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.MatchService;
import br.com.muxe.minigame.api.runtime.MatchState;
import br.com.muxe.minigame.api.runtime.Playing;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.api.settings.SpectatorHotbar;
import br.com.muxe.minigame.api.spectator.Spectating;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MatchListener<S extends MatchState> implements Listener {

    private static final List<String> LOBBY_COMMANDS = List.of("/l", "/lobby", "/hub");
    private static final long ARRIVAL_DELAY_TICKS = 2L;

    private final Plugin plugin;
    private final MuxeCore core;
    private final MatchService<?, S> matches;
    private final GameHandlers<S> handlers;
    private final MatchSettings settings;
    private final MatchRules rules;

    public MatchListener(Plugin plugin, MuxeCore core, MatchService<?, S> matches, GameHandlers<S> handlers) {
        this.plugin = plugin;
        this.core = core;
        this.matches = matches;
        this.handlers = handlers;
        this.settings = matches.environment().settings();
        this.rules = matches.environment().rules();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                matches.arrive(player.getUniqueId(), player.getName());
            }
        }, ARRIVAL_DELAY_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        matches.leave(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!matches.isMatchWorld(event.getPlayer().getLocation()) && matches.isMatchWorld(event.getFrom().getSpawnLocation())) {
            matches.leave(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLobbyCommand(PlayerCommandPreprocessEvent event) {
        String label = event.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);
        if (!LOBBY_COMMANDS.contains(label)) {
            return;
        }
        UUID id = event.getPlayer().getUniqueId();
        matches.of(id)
            .filter(runtime -> runtime.match().leavingWhileReturnable(id))
            .ifPresent(runtime -> event.getPlayer().sendMessage(runtime.message(id, "left-match", "mode", runtime.modeName())));
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Optional<MatchRuntime<S>> found = matches.of(player.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        MatchRuntime<S> runtime = found.get();
        Match match = runtime.match();
        ItemStack item = event.getItem();
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (match.phase().beforeStart()) {
            event.setCancelled(true);
            int held = player.getInventory().getHeldItemSlot();
            if (item != null && rightClick && held == settings.waitingHotbar().leave().slot()) {
                core.network().connectToGroup(player.getUniqueId(), settings.lobbyGroup());
            } else if (item != null && rightClick && held == settings.waitingHotbar().menu().slot()) {
                handlers.openMenu(player);
            }
            return;
        }
        boolean alive = match.isAlive(player.getUniqueId());
        boolean spectating = match.phase() == MatchPhase.PLAYING && !alive;
        if ((spectating || match.phase() == MatchPhase.ENDING) && item != null) {
            Optional<SpectatorHotbar.Action> action = settings.spectatorHotbar().action(player.getInventory().getHeldItemSlot(), item.getType().name());
            if (action.isPresent()) {
                event.setCancelled(true);
                if (rightClick) {
                    spectatorAction(player, runtime, action.get());
                }
                return;
            }
        }
        if (spectating) {
            event.setCancelled(true);
            return;
        }
        if (match.phase() == MatchPhase.PLAYING) {
            playing(runtime, player.getUniqueId()).ifPresent(playing -> handlers.interact(playing, event));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Optional<MatchRuntime<S>> found = matches.of(player.getUniqueId());
        if (found.isEmpty() || !matches.isMatchWorld(event.getBlock().getLocation())) {
            return;
        }
        Optional<Playing<S>> playing = playing(found.get(), player.getUniqueId());
        if (playing.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        handlers.place(playing.get(), event);
        Block block = event.getBlockPlaced();
        if (!event.isCancelled() && block.getType() != Material.AIR) {
            found.get().match().placed(block.getX(), block.getY(), block.getZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Optional<MatchRuntime<S>> found = matches.of(player.getUniqueId());
        if (found.isEmpty() || !matches.isMatchWorld(event.getBlock().getLocation())) {
            return;
        }
        Optional<Playing<S>> playing = playing(found.get(), player.getUniqueId());
        if (playing.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        handlers.breakBlock(playing.get(), event);
        if (event.isCancelled()) {
            return;
        }
        Block block = event.getBlock();
        if (!found.get().mayBreak(player.getUniqueId(), block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            return;
        }
        found.get().match().unplaced(block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplode(EntityExplodeEvent event) {
        Optional<MatchRuntime<S>> found = matches.at(event.getLocation());
        if (found.isEmpty()) {
            if (matches.isMatchWorld(event.getLocation())) {
                event.blockList().clear();
            }
            handlers.exploded(found, event);
            return;
        }
        Match match = found.get().match();
        event.blockList().removeIf(block -> !handlers.explodes(block) || rules.breakOnlyPlaced() && !match.wasPlaced(block.getX(), block.getY(), block.getZ()));
        event.blockList().forEach(block -> match.unplaced(block.getX(), block.getY(), block.getZ()));
        handlers.exploded(found, event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Optional<MatchRuntime<S>> found = matches.of(player.getUniqueId());
        if (found.isEmpty() || !matches.isMatchWorld(player.getLocation())) {
            return;
        }
        MatchRuntime<S> runtime = found.get();
        Match match = runtime.match();
        Optional<Playing<S>> victim = playing(runtime, player.getUniqueId());
        if (victim.isEmpty() || event.getCause() == DamageCause.VOID) {
            event.setCancelled(true);
            return;
        }
        Participant target = victim.get().participant();
        Entity damager = event instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
        Optional<UUID> attacker = damager == null ? Optional.empty() : attacker(damager);
        if (attacker.filter(player.getUniqueId()::equals).isPresent() && handlers.selfDamage(victim.get(), damager, event)) {
            event.setCancelled(true);
            return;
        }
        Optional<Participant> other = attacker.filter(id -> !id.equals(player.getUniqueId())).flatMap(match::player);
        if (attacker.isPresent() && !attacker.get().equals(player.getUniqueId())) {
            boolean direct = damager instanceof Player || damager instanceof Projectile;
            if (other.isEmpty() || other.get().team() == target.team() || direct && !other.get().isAlive()) {
                event.setCancelled(true);
                return;
            }
            if (other.get().isAlive()) {
                match.struck(other.get().id());
            }
        }
        if (target.isProtected(runtime.environment().clock().instant())) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity && attacker.isPresent()) {
            other.filter(Participant::isAlive).ifPresent(hitter -> match.credit().hit(player.getUniqueId(), hitter.id(), runtime.environment().clock().instant()));
            handlers.damaged(victim.get(), attacker.flatMap(match::player), byEntity);
        }
        if (event.getFinalDamage() >= player.getHealth()) {
            event.setCancelled(true);
            runtime.die(player.getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        matches.of(player.getUniqueId()).ifPresent(runtime -> {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setKeepInventory(true);
            Location diedAt = player.getLocation();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.spigot().respawn();
                player.teleport(diedAt);
                runtime.die(player.getUniqueId(), false);
            });
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        Optional<MatchRuntime<S>> found = matches.of(player.getUniqueId());
        if (found.isEmpty() || !matches.isMatchWorld(event.getTo()) || !found.get().belowVoid(event.getTo().getY())) {
            return;
        }
        found.get().fellIntoVoid(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!rules.hunger() && event.getEntity() instanceof Player player && matches.of(player.getUniqueId()).isPresent()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        matches.of(id).ifPresent(runtime -> {
            if (playing(runtime, id).isEmpty() || rules.undroppable().test(event.getItemDrop().getItemStack().getType())) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        matches.of(id).ifPresent(runtime -> {
            Optional<Playing<S>> playing = playing(runtime, id);
            if (playing.isEmpty()) {
                event.setCancelled(true);
                return;
            }
            handlers.pickup(playing.get(), event);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmor(InventoryClickEvent event) {
        if (!rules.moveArmor() && event.getWhoClicked() instanceof Player player && matches.of(player.getUniqueId()).isPresent()
            && event.getSlotType() == SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!rules.crafting() && matches.of(event.getWhoClicked().getUniqueId()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        if (!rules.enterBeds() && matches.isMatchWorld(event.getBed().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpectatorInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            return;
        }
        UUID id = player.getUniqueId();
        matches.of(id)
            .filter(runtime -> runtime.match().phase() == MatchPhase.ENDING || runtime.match().phase() == MatchPhase.PLAYING && !runtime.match().isAlive(id))
            .ifPresent(runtime -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        matches.chatView(id).ifPresent(view -> {
            boolean global = matches.isShouting(id);
            Set<UUID> audience = view.audience(global);
            event.getRecipients().removeIf(recipient -> !audience.contains(recipient.getUniqueId()));
            String shout = global ? matches.environment().messages().get("global-prefix") : "";
            String badge = matches.chatBadge(id);
            String shown = badge.isEmpty() ? "" : Colors.translate(badge) + " ";
            String template = shout + shown + Colors.translate(view.prefix()) + core.messages().get("chat-format");
            event.setFormat(core.permissions().display(id).chatFormat(template));
        });
    }

    private Optional<UUID> attacker(Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player.getUniqueId());
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return Optional.of(shooter.getUniqueId());
        }
        return handlers.attacker(damager);
    }

    private Optional<Playing<S>> playing(MatchRuntime<S> runtime, UUID id) {
        if (runtime.match().phase() != MatchPhase.PLAYING) {
            return Optional.empty();
        }
        return runtime.match().player(id).filter(Participant::isAlive).map(participant -> new Playing<>(runtime, participant));
    }

    private void spectatorAction(Player player, MatchRuntime<S> runtime, SpectatorHotbar.Action action) {
        switch (action) {
            case TELEPORTER -> Spectating.openTeleporter(runtime, player);
            case RANDOM_PLAYER -> Spectating.teleport(runtime, player, false);
            case PLAYER_IN_COMBAT -> Spectating.teleport(runtime, player, true);
            case PLAY_AGAIN -> handlers.playAgain(player, runtime.match().mode().id());
            case LEAVE -> core.network().connectToGroup(player.getUniqueId(), settings.lobbyGroup());
        }
    }
}
