package br.com.muxe.minigame.bedwars.cosmetic.preview;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import br.com.muxe.core.bukkit.cosmetic.stage.AnimationPlayer.Playback;
import br.com.muxe.core.bukkit.cosmetic.stage.AnimationPlayer;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.nms.BlockMask;
import br.com.muxe.core.bukkit.scoreboard.Sidebar;
import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.api.cosmetic.catalog.Cosmetic;
import br.com.muxe.minigame.api.cosmetic.menu.CosmeticMenus.PreviewGate;
import br.com.muxe.minigame.bedwars.cosmetic.animation.BedAnimations;
import br.com.muxe.minigame.engine.world.server.VoidGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private static final long CHUNKS_ARRIVED = 10;

    private final Plugin plugin;
    private final BuildMode buildMode;
    private final Optional<BlockMask> blocks;
    private final Supplier<PreviewSettings> settings;
    private final Function<UUID, PreviewSettings> texts;
    private final BedAnimations animations;
    private final AnimationPlayer animator;
    private final MessageCatalog messages;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private Stage stage;
    private boolean listening;

    public BedPreview(Plugin plugin, BuildMode buildMode, Optional<BlockMask> blocks, Supplier<PreviewSettings> settings, Function<UUID, PreviewSettings> texts,
                      BedAnimations animations, MessageCatalog messages) {
        this.plugin = plugin;
        this.buildMode = buildMode;
        this.blocks = blocks;
        this.settings = settings;
        this.texts = texts;
        this.animations = animations;
        this.animator = new AnimationPlayer(plugin);
        this.messages = messages;
    }

    public boolean start() {
        return open();
    }

    public boolean refresh() {
        return open();
    }

    @SuppressWarnings("deprecation")
    private boolean open() {
        PreviewSettings next = settings.get();
        if (!plugin.getServer().getWorldContainer().toPath().resolve(next.world()).toFile().isDirectory()) {
            plugin.getLogger().warning("The bed preview is off: there is no world folder named " + next.world());
            return false;
        }
        if (stage != null && stage.sameSceneAs(next)) {
            stage = stage.with(next);
            return true;
        }
        World world = new WorldCreator(next.world()).generator(new VoidGenerator()).generateStructures(false).createWorld();
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(true);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doFireTick", "false");
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
        List<BedHalf> halves = new ArrayList<>();
        for (Vec half : List.of(next.bed().foot(), next.bed().head())) {
            Block block = Stage.blockAt(world, half);
            if (block.getType() == Material.BED_BLOCK) {
                halves.add(new BedHalf(block.getLocation(), block.getData()));
            }
        }
        stage = new Stage(next, world, halves, new BedBreakers());
        if (!listening) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listening = true;
        }
        return true;
    }

    public void stop() {
        List.copyOf(sessions.keySet()).forEach(id -> Optional.ofNullable(plugin.getServer().getPlayer(id)).ifPresent(viewer -> finish(viewer, false)));
    }

    public boolean previewing(Player viewer) {
        return sessions.containsKey(viewer.getUniqueId());
    }

    @Override
    public void preview(Player viewer, Cosmetic cosmetic, Consumer<Player> back) {
        Stage active = stage;
        Optional<BedAnimation> animation = animations.find(cosmetic.id());
        if (active == null || animation.isEmpty()) {
            viewer.sendMessage(messages.get(viewer, active == null ? "preview-only-lobby" : "cosmetic-no-preview"));
            return;
        }
        if (previewing(viewer)) {
            viewer.sendMessage(messages.get(viewer, "preview-busy"));
            return;
        }
        PreviewSettings config = active.settings();
        PreviewSettings shown = texts.apply(viewer.getUniqueId());
        Session session = new Session(viewer, animation.get(), back, active);
        sessions.put(viewer.getUniqueId(), session);
        viewer.getInventory().clear();
        viewer.getInventory().setArmorContents(new ItemStack[4]);
        viewer.getInventory().setItem(config.back().slot(), backItem(config, shown));
        buildMode.releaseGameMode(viewer);
        viewer.setGameMode(GameMode.SURVIVAL);
        viewer.setAllowFlight(false);
        viewer.teleport(new Location(active.world(), config.x(), config.y(), config.z(), config.yaw(), config.pitch()));
        if (active.breakers().broken()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (previewing(viewer) && active.breakers().broken() && !active.breakers().breaker(viewer.getUniqueId())) {
                    showBed(active, viewer);
                }
            }, CHUNKS_ARRIVED);
        }
        for (Session other : sessions.values()) {
            if (other != session) {
                session.hide(other.viewer);
                other.hide(viewer);
            }
        }
        Sidebar sidebar = Sidebar.create(viewer);
        sidebar.title(shown.title());
        sidebar.lines(shown.lines(cosmetic.name()));
        String bar = Colors.translate(shown.actionBar());
        session.actionBar = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> ActionBar.send(viewer, bar), 0L, TICKS_PER_SECOND);
        session.timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(viewer, true), config.timeout().toSeconds() * TICKS_PER_SECOND);
    }

    private ItemStack backItem(PreviewSettings config, PreviewSettings shown) {
        Material material = Material.matchMaterial(config.back().material());
        return ItemBuilder.of(material == null ? Material.BED : material).name(shown.back().name()).build();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        PreviewSettings config = session.stage.settings();
        if (config.goesBack(rightClick, event.getPlayer().getInventory().getHeldItemSlot())) {
            finish(event.getPlayer(), true);
            return;
        }
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || block == null || session.playback != null
            || !config.isBed(block.getX(), block.getY(), block.getZ())) {
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
            plugin.getServer().getScheduler().runTask(plugin, () -> hideBed(session));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player clicker && previewing(clicker)) {
            event.setCancelled(true);
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
            finish(event.getPlayer(), false);
        }
    }

    private void play(Session session) {
        Player viewer = session.viewer;
        Stage active = session.stage;
        session.actionBar.cancel();
        ActionBar.send(viewer, "");
        session.timeout.cancel();
        if (active.breakers().broke(viewer.getUniqueId())) {
            takeBed(active);
        }
        hideBed(session);
        plugin.getServer().getScheduler().runTask(plugin, () -> hideBed(session));
        Location at = viewer.getLocation();
        Vec breaker = new Vec(at.getX(), at.getY(), at.getZ());
        session.playback = animator.play(session.animation, active.world(), active.settings().bed(), breaker, () -> List.of(viewer), () -> {
            hideBed(session);
            session.timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(viewer, true),
                Math.max(1, active.settings().returnDelay().toMillis() / 50));
        });
    }

    @SuppressWarnings("deprecation")
    private void hideBed(Session session) {
        Player viewer = session.viewer;
        if (!previewing(viewer)) {
            return;
        }
        Stage active = session.stage;
        for (Vec half : List.of(active.settings().bed().foot(), active.settings().bed().head())) {
            Location at = new Location(active.world(), half.x(), half.y(), half.z());
            blocks.ifPresentOrElse(mask -> mask.hide(viewer, at, Material.AIR, (byte) 0), () -> viewer.sendBlockChange(at, Material.AIR, (byte) 0));
        }
    }

    private void finish(Player viewer, boolean reopen) {
        Session session = sessions.remove(viewer.getUniqueId());
        if (session == null) {
            return;
        }
        Stage active = session.stage;
        session.cancel();
        boolean lastBreaker = active.breakers().left(viewer.getUniqueId());
        viewer.teleport(session.from);
        for (Vec half : List.of(active.settings().bed().foot(), active.settings().bed().head())) {
            Block block = Stage.blockAt(active.world(), half);
            blocks.ifPresentOrElse(mask -> mask.reveal(viewer, block.getLocation()), () -> sendBlock(viewer, block));
        }
        if (lastBreaker) {
            putBed(active);
        }
        viewer.getInventory().setContents(session.contents);
        viewer.getInventory().setArmorContents(session.armor);
        viewer.setGameMode(session.mode);
        buildMode.lockGameMode(viewer);
        viewer.setAllowFlight(session.allowFlight);
        viewer.setScoreboard(session.scoreboard);
        viewer.updateInventory();
        for (Session other : sessions.values()) {
            other.unhide(viewer);
        }
        session.restoreHidden();
        if (reopen) {
            session.back.accept(viewer);
        }
    }

    @SuppressWarnings("deprecation")
    private static void sendBlock(Player viewer, Block block) {
        if (viewer.getWorld().equals(block.getWorld())) {
            viewer.sendBlockChange(block.getLocation(), block.getType(), block.getData());
        }
    }

    @SuppressWarnings("deprecation")
    private void takeBed(Stage active) {
        List<Player> others = active.world().getPlayers().stream().filter(player -> !active.breakers().breaker(player.getUniqueId())).toList();
        if (blocks.isPresent()) {
            others.forEach(other -> showBed(active, other));
        }
        for (BedHalf half : active.halves()) {
            half.at().getBlock().setTypeIdAndData(Material.AIR.getId(), (byte) 0, false);
        }
        if (blocks.isEmpty()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> others.stream().filter(Player::isOnline).forEach(other -> showBed(active, other)));
        }
    }

    @SuppressWarnings("deprecation")
    private void showBed(Stage active, Player viewer) {
        for (BedHalf half : active.halves()) {
            blocks.ifPresentOrElse(mask -> mask.hide(viewer, half.at(), Material.BED_BLOCK, half.data()),
                () -> viewer.sendBlockChange(half.at(), Material.BED_BLOCK, half.data()));
        }
    }

    @SuppressWarnings("deprecation")
    private void putBed(Stage active) {
        for (BedHalf half : active.halves()) {
            half.at().getBlock().setTypeIdAndData(Material.BED_BLOCK.getId(), half.data(), false);
        }
        for (Player player : active.world().getPlayers()) {
            for (BedHalf half : active.halves()) {
                blocks.ifPresentOrElse(mask -> mask.reveal(player, half.at()), () -> sendBlock(player, half.at().getBlock()));
            }
        }
    }

    private record BedHalf(Location at, byte data) {
    }

    private record Stage(PreviewSettings settings, World world, List<BedHalf> halves, BedBreakers breakers) {

        boolean sameSceneAs(PreviewSettings next) {
            return settings.world().equals(next.world()) && settings.bed().equals(next.bed());
        }

        Stage with(PreviewSettings next) {
            return new Stage(next, world, halves, breakers);
        }

        static Block blockAt(World world, Vec half) {
            return world.getBlockAt((int) Math.floor(half.x()), (int) Math.floor(half.y()), (int) Math.floor(half.z()));
        }
    }

    private static final class Session {

        private final Player viewer;
        private final BedAnimation animation;
        private final Consumer<Player> back;
        private final Stage stage;
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

        Session(Player viewer, BedAnimation animation, Consumer<Player> back, Stage stage) {
            this.viewer = viewer;
            this.animation = animation;
            this.back = back;
            this.stage = stage;
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
