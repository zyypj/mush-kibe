package br.com.muxe.minigame.bedwars.listener;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.room.Room;
import br.com.muxe.minigame.bedwars.room.RoomPlayer;
import br.com.muxe.minigame.bedwars.room.RoomRuntime;
import br.com.muxe.minigame.bedwars.room.RoomService;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

public final class MatchListener implements Listener {

    private static final List<String> LOBBY_COMMANDS = List.of("/l", "/lobby", "/hub");
    private static final int TNT_FUSE_TICKS = 52;
    private static final double FIREBALL_SPEED = 1.5;

    private final Plugin plugin;
    private final MuxeCore core;
    private final RoomService rooms;
    private final BedWarsSettings settings;
    private final MessageCatalog messages;
    private final Consumer<Player> openMenu;
    private final Map<UUID, UUID> explosives = new HashMap<>();

    public MatchListener(Plugin plugin, MuxeCore core, RoomService rooms, BedWarsSettings settings, MessageCatalog messages, Consumer<Player> openMenu) {
        this.openMenu = openMenu;
        this.plugin = plugin;
        this.core = core;
        this.rooms = rooms;
        this.settings = settings;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                rooms.arrive(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        rooms.leave(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!rooms.isMatchWorld(event.getPlayer().getLocation()) && rooms.isMatchWorld(event.getFrom().getSpawnLocation())) {
            rooms.leave(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLobbyCommand(PlayerCommandPreprocessEvent event) {
        String label = event.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);
        if (!LOBBY_COMMANDS.contains(label)) {
            return;
        }
        rooms.roomOf(event.getPlayer().getUniqueId())
            .filter(runtime -> runtime.leavingWithBed(event.getPlayer()))
            .ifPresent(runtime -> event.getPlayer().sendMessage(messages.get("left-match", "mode", runtime.room().mode().displayName())));
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Optional<RoomRuntime> found = rooms.roomOf(player.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        Room room = found.get().room();
        ItemStack item = event.getItem();
        if (room.phase() == Room.Phase.WAITING || room.phase() == Room.Phase.STARTING) {
            event.setCancelled(true);
            boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
            int held = player.getInventory().getHeldItemSlot();
            if (item != null && rightClick && held == settings.hotbar().leave().slot()) {
                core.network().connectToGroup(player.getUniqueId(), settings.lobbyGroup());
            } else if (item != null && rightClick && held == settings.hotbar().menu().slot()) {
                openMenu.accept(player);
            }
            return;
        }
        if (room.phase() != Room.Phase.PLAYING || item == null) {
            return;
        }
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (rightClick && item.getType() == Material.FIREBALL) {
            event.setCancelled(true);
            consumeOne(player);
            Fireball fireball = player.launchProjectile(Fireball.class);
            fireball.setVelocity(player.getLocation().getDirection().multiply(FIREBALL_SPEED));
            fireball.setIsIncendiary(false);
            fireball.setYield(2.0f);
            explosives.put(fireball.getUniqueId(), player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Optional<RoomRuntime> found = rooms.roomOf(player.getUniqueId());
        if (found.isEmpty() || !rooms.isMatchWorld(event.getBlock().getLocation())) {
            return;
        }
        RoomRuntime runtime = found.get();
        if (runtime.room().phase() != Room.Phase.PLAYING || !alive(runtime, player)) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getBlockPlaced();
        if (!runtime.mayPlace(block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(messages.get("cannot-place"));
            return;
        }
        if (block.getType() == Material.TNT) {
            block.setType(Material.AIR, false);
            TNTPrimed tnt = block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(TNT_FUSE_TICKS);
            explosives.put(tnt.getUniqueId(), player.getUniqueId());
            return;
        }
        runtime.placed(block);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Optional<RoomRuntime> found = rooms.roomOf(player.getUniqueId());
        if (found.isEmpty() || !rooms.isMatchWorld(event.getBlock().getLocation())) {
            return;
        }
        RoomRuntime runtime = found.get();
        Block block = event.getBlock();
        if (runtime.room().phase() != Room.Phase.PLAYING || !alive(runtime, player)) {
            event.setCancelled(true);
            return;
        }
        if (block.getType() == Material.BED_BLOCK) {
            event.setCancelled(true);
            runtime.breakBed(player, block);
            return;
        }
        if (!runtime.room().wasPlaced(block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            return;
        }
        runtime.room().unplaced(block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplode(EntityExplodeEvent event) {
        Optional<RoomRuntime> found = rooms.roomAt(event.getLocation());
        UUID exploded = event.getEntity().getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> explosives.remove(exploded), 1L);
        if (found.isEmpty()) {
            if (rooms.isMatchWorld(event.getLocation())) {
                event.blockList().clear();
            }
            return;
        }
        Room room = found.get().room();
        event.blockList().removeIf(block -> block.getType() == Material.GLASS || block.getType() == Material.STAINED_GLASS
            || !room.wasPlaced(block.getX(), block.getY(), block.getZ()));
        event.blockList().forEach(block -> room.unplaced(block.getX(), block.getY(), block.getZ()));
        event.setYield(0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Optional<RoomRuntime> found = rooms.roomOf(player.getUniqueId());
        if (found.isEmpty() || !rooms.isMatchWorld(player.getLocation())) {
            return;
        }
        RoomRuntime runtime = found.get();
        Room room = runtime.room();
        if (room.phase() != Room.Phase.PLAYING || !alive(runtime, player) || event.getCause() == DamageCause.VOID) {
            event.setCancelled(true);
            return;
        }
        RoomPlayer roomPlayer = room.player(player.getUniqueId()).orElseThrow();
        if (roomPlayer.isProtected(core.clock().instant())) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Optional<Player> attacker = attacker(byEntity.getDamager());
            if (attacker.isPresent()) {
                Optional<RoomPlayer> other = room.player(attacker.get().getUniqueId());
                if (other.isPresent() && other.get().team() == roomPlayer.team() && !attacker.get().equals(player)) {
                    event.setCancelled(true);
                    return;
                }
                if (other.isPresent() && other.get().isAlive()) {
                    room.credit().hit(player.getUniqueId(), attacker.get().getUniqueId(), core.clock().instant());
                }
            }
        }
        if (event.getFinalDamage() >= player.getHealth()) {
            event.setCancelled(true);
            runtime.die(player, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        rooms.roomOf(player.getUniqueId()).ifPresent(runtime -> {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setKeepInventory(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.spigot().respawn();
                runtime.die(player, false);
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
        Optional<RoomRuntime> found = rooms.roomOf(player.getUniqueId());
        if (found.isEmpty() || !rooms.isMatchWorld(event.getTo())) {
            return;
        }
        RoomRuntime runtime = found.get();
        if (!runtime.belowVoid(event.getTo())) {
            return;
        }
        Room room = runtime.room();
        switch (room.phase()) {
            case WAITING, STARTING -> player.teleport(runtime.waitingSpawn());
            case PLAYING -> {
                if (alive(runtime, player)) {
                    runtime.die(player, true);
                } else {
                    player.teleport(runtime.spectatorSpawn());
                }
            }
            case ENDING -> player.teleport(runtime.spectatorSpawn());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && rooms.roomOf(player.getUniqueId()).isPresent()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        rooms.roomOf(event.getPlayer().getUniqueId()).ifPresent(runtime -> {
            Material type = event.getItemDrop().getItemStack().getType();
            if (runtime.room().phase() != Room.Phase.PLAYING || type == Material.WOOD_SWORD || type.name().endsWith("_PICKAXE")
                || type.name().endsWith("_AXE") || type == Material.SHEARS) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        rooms.roomOf(player.getUniqueId()).ifPresent(runtime -> {
            if (runtime.room().phase() != Room.Phase.PLAYING || !alive(runtime, player)) {
                event.setCancelled(true);
                return;
            }
            Item item = event.getItem();
            if (!item.hasMetadata("muxe-forge")) {
                return;
            }
            RoomPlayer picker = runtime.room().player(player.getUniqueId()).orElseThrow();
            ItemStack stack = item.getItemStack();
            double radius = settings.forges().splitRadius();
            for (Entity nearby : player.getNearbyEntities(radius, radius, 2.0)) {
                if (nearby instanceof Player mate && !mate.equals(player)) {
                    runtime.room().player(mate.getUniqueId())
                        .filter(other -> other.team() == picker.team() && other.isAlive())
                        .ifPresent(other -> mate.getInventory().addItem(new ItemStack(stack.getType(), stack.getAmount())));
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmor(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && rooms.roomOf(player.getUniqueId()).isPresent()
            && event.getSlotType() == SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (rooms.roomOf(event.getWhoClicked().getUniqueId()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        if (rooms.isMatchWorld(event.getBed().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Optional<RoomRuntime> found = rooms.roomOf(player.getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        RoomRuntime runtime = found.get();
        List<Player> audience = runtime.online();
        event.getRecipients().clear();
        event.getRecipients().addAll(audience);
        Optional<RoomPlayer> roomPlayer = runtime.room().player(player.getUniqueId());
        String team = roomPlayer.map(RoomPlayer::team)
            .filter(color -> runtime.room().phase() == Room.Phase.PLAYING || runtime.room().phase() == Room.Phase.ENDING)
            .map(color -> color.color() + "[" + color.letter() + "] ")
            .orElse("");
        String template = Colors.translate(team) + core.messages().get("chat-format");
        event.setFormat(core.permissions().display(player.getUniqueId()).chatFormat(template));
    }

    private boolean alive(RoomRuntime runtime, Player player) {
        return runtime.room().player(player.getUniqueId()).map(RoomPlayer::isAlive).orElse(false);
    }

    private Optional<Player> attacker(Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player);
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return Optional.of(shooter);
        }
        if (damager instanceof TNTPrimed || damager instanceof Fireball) {
            UUID owner = explosives.get(damager.getUniqueId());
            return owner == null ? Optional.empty() : Optional.ofNullable(plugin.getServer().getPlayer(owner));
        }
        return Optional.empty();
    }

    private static void consumeOne(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}
