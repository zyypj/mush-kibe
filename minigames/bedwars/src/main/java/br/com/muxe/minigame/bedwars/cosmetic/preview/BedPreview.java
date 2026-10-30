package br.com.muxe.minigame.bedwars.cosmetic.preview;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import br.com.muxe.core.bukkit.cosmetic.stage.AnimationPlayer;
import br.com.muxe.core.bukkit.cosmetic.stage.AnimationPlayer.Playback;
import br.com.muxe.core.bukkit.scoreboard.Sidebar;
import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.cosmetic.animation.BedAnimations;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.BedCosmetic;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticMenus.PreviewGate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

public final class BedPreview implements PreviewGate, Listener {

    private static final long TICKS_PER_SECOND = 20;

    private final Plugin plugin;
    private final PreviewSettings settings;
    private final BedAnimations animations;
    private final AnimationPlayer animator;
    private final MessageCatalog messages;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private World world;

    public BedPreview(Plugin plugin, PreviewSettings settings, BedAnimations animations, MessageCatalog messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.animations = animations;
        this.animator = new AnimationPlayer(plugin);
        this.messages = messages;
    }

    public boolean start() {
        if (!plugin.getServer().getWorldContainer().toPath().resolve(settings.world()).toFile().isDirectory()) {
            plugin.getLogger().warning("The bed preview is off: there is no world folder named " + settings.world());
            return false;
        }
        world = new WorldCreator(settings.world()).createWorld();
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(true);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doFireTick", "false");
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        return true;
    }

    public void stop() {
        List.copyOf(sessions.keySet()).forEach(id -> Optional.ofNullable(plugin.getServer().getPlayer(id)).ifPresent(this::finish));
    }

    public boolean previewing(Player viewer) {
        return sessions.containsKey(viewer.getUniqueId());
    }

    @Override
    public void preview(Player viewer, BedCosmetic cosmetic) {
        Optional<BedAnimation> animation = animations.find(cosmetic.id());
        if (world == null || animation.isEmpty()) {
            viewer.sendMessage(messages.get(world == null ? "preview-only-lobby" : "cosmetic-no-preview"));
            return;
        }
        if (previewing(viewer)) {
            viewer.sendMessage(messages.get("preview-busy"));
            return;
        }
        Session session = new Session(viewer, animation.get());
        sessions.put(viewer.getUniqueId(), session);
        viewer.getInventory().clear();
        viewer.getInventory().setArmorContents(new ItemStack[4]);
        viewer.setGameMode(GameMode.SURVIVAL);
        viewer.setAllowFlight(false);
        viewer.teleport(new Location(world, settings.x(), settings.y(), settings.z(), settings.yaw(), settings.pitch()));
        for (Session other : sessions.values()) {
            if (other != session) {
                session.hide(other.viewer);
                other.hide(viewer);
            }
        }
        Sidebar sidebar = Sidebar.create(viewer);
        sidebar.title(settings.title());
        sidebar.lines(settings.lines(cosmetic.name()));
        String bar = Colors.translate(settings.actionBar());
        session.actionBar = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> ActionBar.send(viewer, bar), 0L, TICKS_PER_SECOND);
        session.timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(viewer), settings.timeout().toSeconds() * TICKS_PER_SECOND);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || block == null || session.playback != null
            || !settings.isBed(block.getX(), block.getY(), block.getZ())) {
            return;
        }
        play(session);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        if (session.playback != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> hideBed(session.viewer));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (previewing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player victim && previewing(victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player hungry && previewing(hungry)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (previewing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(PlayerPickupItemEvent event) {
        if (previewing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (previewing(event.getPlayer())) {
            finish(event.getPlayer());
        }
    }

    private void play(Session session) {
        Player viewer = session.viewer;
        session.actionBar.cancel();
        ActionBar.send(viewer, "");
        session.timeout.cancel();
        hideBed(viewer);
        Location at = viewer.getLocation();
        Vec breaker = new Vec(at.getX(), at.getY(), at.getZ());
        session.playback = animator.play(session.animation, world, settings.bed(), breaker, () -> List.of(viewer), () ->
            session.timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(viewer), Math.max(1, settings.returnDelay().toMillis() / 50)));
    }

    @SuppressWarnings("deprecation")
    private void hideBed(Player viewer) {
        if (!previewing(viewer)) {
            return;
        }
        for (Vec half : List.of(settings.bed().foot(), settings.bed().head())) {
            viewer.sendBlockChange(new Location(world, half.x(), half.y(), half.z()), Material.AIR, (byte) 0);
        }
    }

    private void finish(Player viewer) {
        Session session = sessions.remove(viewer.getUniqueId());
        if (session == null) {
            return;
        }
        session.cancel();
        for (Vec half : List.of(settings.bed().foot(), settings.bed().head())) {
            Block block = world.getBlockAt((int) Math.floor(half.x()), (int) Math.floor(half.y()), (int) Math.floor(half.z()));
            sendBlock(viewer, block);
        }
        viewer.teleport(session.from);
        viewer.getInventory().setContents(session.contents);
        viewer.getInventory().setArmorContents(session.armor);
        viewer.setGameMode(session.mode);
        viewer.setAllowFlight(session.allowFlight);
        viewer.setScoreboard(session.scoreboard);
        viewer.updateInventory();
        for (Session other : sessions.values()) {
            other.unhide(viewer);
        }
        session.restoreHidden();
    }

    @SuppressWarnings("deprecation")
    private static void sendBlock(Player viewer, Block block) {
        viewer.sendBlockChange(block.getLocation(), block.getType(), block.getData());
    }

    private static final class Session {

        private final Player viewer;
        private final BedAnimation animation;
        private final Location from;
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final GameMode mode;
        private final boolean allowFlight;
        private final Scoreboard scoreboard;
        private final List<Player> hidden = new ArrayList<>();
        private BukkitTask actionBar;
        private BukkitTask timeout;
        private Playback playback;

        Session(Player viewer, BedAnimation animation) {
            this.viewer = viewer;
            this.animation = animation;
            this.from = viewer.getLocation();
            this.contents = viewer.getInventory().getContents();
            this.armor = viewer.getInventory().getArmorContents();
            this.mode = viewer.getGameMode();
            this.allowFlight = viewer.getAllowFlight();
            this.scoreboard = viewer.getScoreboard();
        }

        void hide(Player other) {
            if (viewer.canSee(other)) {
                viewer.hidePlayer(other);
                hidden.add(other);
            }
        }

        void unhide(Player other) {
            if (hidden.remove(other) && other.isOnline()) {
                viewer.showPlayer(other);
            }
        }

        void restoreHidden() {
            for (Player other : hidden) {
                if (other.isOnline()) {
                    viewer.showPlayer(other);
                }
            }
            hidden.clear();
        }

        void cancel() {
            if (actionBar != null) {
                actionBar.cancel();
            }
            if (timeout != null) {
                timeout.cancel();
            }
            if (playback != null) {
                playback.cancel();
            }
        }
    }
}
