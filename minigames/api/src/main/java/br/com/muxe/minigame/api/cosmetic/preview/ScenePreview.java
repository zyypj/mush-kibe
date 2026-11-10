package br.com.muxe.minigame.api.cosmetic.preview;

import br.com.muxe.core.bukkit.buildmode.BuildMode;
import br.com.muxe.core.bukkit.cosmetic.animation.Scene;
import br.com.muxe.core.bukkit.cosmetic.animation.SceneAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.SignedSkin;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import br.com.muxe.core.bukkit.cosmetic.stage.AnimationPlayer.Playback;
import br.com.muxe.core.bukkit.cosmetic.stage.AnimationPlayer;
import br.com.muxe.core.bukkit.effect.SoundCue;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.nms.PlayerSkins;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import br.com.muxe.core.bukkit.scoreboard.Sidebar;
import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.minigame.api.cosmetic.SceneCosmetics;
import br.com.muxe.minigame.api.cosmetic.animation.SceneAnimations;
import br.com.muxe.minigame.api.cosmetic.catalog.Cosmetic;
import br.com.muxe.minigame.api.cosmetic.menu.CosmeticMenus.PreviewGate;
import br.com.muxe.minigame.engine.world.server.VoidGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

public final class ScenePreview implements PreviewGate, Listener {

    private static final long TICKS_PER_SECOND = 20;
    private static final int COUNTDOWN_STAY = 40;
    private static final String SUBTITLE = " ";
    private static final String TARGET_PREFIX = "previa-alvo:";
    private static final int HOTBAR_SIZE = 9;

    private final Plugin plugin;
    private final BuildMode buildMode;
    private final Supplier<ScenePreviewSettings> settings;
    private final Optional<SceneAnimations> animations;
    private final Optional<KitPlay> kitPlay;
    private final Consumer<Player> onScene;
    private final Consumer<Player> onLeave;
    private final Optional<SoundCue> countdownSound;
    private final Predicate<Player> busyElsewhere;
    private final AnimationPlayer animator;
    private final MessageCatalog messages;
    private final Localized<ScenePreviewSettings> translated;
    private final LanguageLookup languages;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private Stage stage;
    private boolean listening;
    private NpcService targets;
    private final Map<String, String> targetNames = new HashMap<>();

    public ScenePreview(Plugin plugin, BuildMode buildMode, Supplier<ScenePreviewSettings> settings, SceneAnimations animations, Consumer<Player> onScene,
                        Consumer<Player> onLeave, Optional<SoundCue> countdownSound, Predicate<Player> busyElsewhere, MessageCatalog messages,
                        Localized<ScenePreviewSettings> translated, LanguageLookup languages) {
        this(plugin, buildMode, settings, Optional.of(animations), Optional.empty(), onScene, onLeave, countdownSound, busyElsewhere, messages, translated, languages);
    }

    public ScenePreview(Plugin plugin, BuildMode buildMode, Supplier<ScenePreviewSettings> settings, KitPlay kitPlay, Predicate<Player> busyElsewhere,
                        MessageCatalog messages, Localized<ScenePreviewSettings> translated, LanguageLookup languages) {
        this(plugin, buildMode, settings, Optional.empty(), Optional.of(kitPlay), player -> {
        }, player -> {
        }, Optional.empty(), busyElsewhere, messages, translated, languages);
    }

    private ScenePreview(Plugin plugin, BuildMode buildMode, Supplier<ScenePreviewSettings> settings, Optional<SceneAnimations> animations, Optional<KitPlay> kitPlay,
                         Consumer<Player> onScene, Consumer<Player> onLeave, Optional<SoundCue> countdownSound, Predicate<Player> busyElsewhere,
                         MessageCatalog messages, Localized<ScenePreviewSettings> translated, LanguageLookup languages) {
        this.translated = translated;
        this.languages = languages;
        this.plugin = plugin;
        this.buildMode = buildMode;
        this.settings = settings;
        this.animations = animations;
        this.kitPlay = kitPlay;
        this.onScene = onScene;
        this.onLeave = onLeave;
        this.countdownSound = countdownSound;
        this.busyElsewhere = busyElsewhere;
        this.animator = new AnimationPlayer(plugin);
        this.messages = messages;
    }

    public boolean start() {
        return open();
    }

    public boolean refresh() {
        return open();
    }

    private boolean open() {
        ScenePreviewSettings next = settings.get();
        if (!plugin.getServer().getWorldContainer().toPath().resolve(next.world()).toFile().isDirectory()) {
            plugin.getLogger().warning("The preview is off: there is no world folder named " + next.world());
            return false;
        }
        World world = stage != null && stage.world().getName().equals(next.world()) ? stage.world() : load(next.world());
        stage = new Stage(next, world);
        if (!listening) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listening = true;
        }
        if (next.target().isPresent() && targets == null) {
            targets = NpcService.start(plugin, npc -> Optional.ofNullable(targetNames.get(npc.id())).map(List::of).orElse(List.of()));
            targets.visibleTo((viewer, npc) -> npc.id().equals(TARGET_PREFIX + viewer.getUniqueId()));
            targets.onClick(this::hit);
        }
        return true;
    }

    private World load(String name) {
        World world = new WorldCreator(name).generator(new VoidGenerator()).generateStructures(false).createWorld();
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(true);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doFireTick", "false");
        return world;
    }

    public void stop() {
        List.copyOf(sessions.keySet()).forEach(id -> Optional.ofNullable(plugin.getServer().getPlayer(id)).ifPresent(viewer -> finish(viewer, false)));
        if (targets != null) {
            targets.stop();
        }
    }

    public boolean previewing(Player viewer) {
        return sessions.containsKey(viewer.getUniqueId());
    }

    @Override
    public void preview(Player viewer, Cosmetic cosmetic, Consumer<Player> back) {
        Stage active = stage;
        Optional<SceneAnimation> animation = animations.flatMap(found -> found.find(cosmetic.id()));
        boolean covered = kitPlay.map(play -> play.covers(cosmetic)).orElse(animation.isPresent());
        if (active == null || !covered) {
            viewer.sendMessage(messages.get(languages.of(viewer.getUniqueId()), active == null ? "preview-only-lobby" : "cosmetic-no-preview"));
            return;
        }
        if (previewing(viewer) || busyElsewhere.test(viewer)) {
            viewer.sendMessage(messages.get(languages.of(viewer.getUniqueId()), "preview-busy"));
            return;
        }
        ScenePreviewSettings config = active.settings();
        ScenePreviewSettings shown = shown(viewer, config);
        World world = active.world();
        Session session = new Session(viewer, back, cosmetic, animation, active);
        sessions.put(viewer.getUniqueId(), session);
        viewer.getInventory().clear();
        viewer.getInventory().setArmorContents(new ItemStack[4]);
        viewer.getInventory().setItem(config.back().slot(), backItem(shown));
        buildMode.releaseGameMode(viewer);
        viewer.setGameMode(GameMode.ADVENTURE);
        viewer.setAllowFlight(false);
        Vec spawn = SceneCosmetics.onGround(world, new Location(world, config.x(), config.y(), config.z()));
        viewer.teleport(new Location(world, spawn.x(), spawn.y(), spawn.z(), config.yaw(), config.pitch()));
        for (Session other : sessions.values()) {
            if (other != session) {
                session.hide(other.viewer);
                other.hide(viewer);
            }
        }
        Sidebar sidebar = Sidebar.create(viewer);
        sidebar.title(shown.title());
        sidebar.lines(shown.lines(cosmetic.name()));
        session.timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(viewer, true), config.timeout().toSeconds() * TICKS_PER_SECOND);
        if (config.kit().isPresent()) {
            giveKit(session, config.kit().get());
            return;
        }
        if (config.target().isPresent() && targets != null) {
            placeTarget(session, shown.target().orElse(config.target().get()));
            return;
        }
        for (int index = 0; index < shown.countdown().size(); index++) {
            String text = Colors.translate(shown.countdown().get(index));
            session.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Title.builder().title(text).subtitle(SUBTITLE).timings(0, COUNTDOWN_STAY, 0).build().send(viewer);
                countdownSound.ifPresent(cue -> cue.play(viewer));
            }, (long) index * config.countdownTicks()));
        }
        session.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> play(session, SceneCosmetics.onGround(world, viewer.getLocation()),
            SceneCosmetics.facing(viewer.getLocation()), Optional.empty(), List.of(viewer.getUniqueId())), Math.max(1, config.sceneStart())));
    }

    private ScenePreviewSettings shown(Player viewer, ScenePreviewSettings live) {
        Language language = languages.of(viewer.getUniqueId());
        return language == Language.DEFAULT ? live : translated.in(language);
    }

    private void placeTarget(Session session, ScenePreviewSettings.Target target) {
        ScenePreviewSettings config = session.stage.settings();
        World world = session.stage.world();
        Vec ahead = config.targetAt();
        Vec at = SceneCosmetics.onGround(world, new Location(world, ahead.x(), ahead.y(), ahead.z()));
        Location spot = new Location(world, at.x(), at.y(), at.z(), config.yaw() + 180, 0);
        session.target = TARGET_PREFIX + session.viewer.getUniqueId();
        targetNames.put(session.target, target.name(ThreadLocalRandom.current().nextInt()));
        session.targetSkin = skin(session.viewer);
        targets.put(new Npc(session.target, spot, session.targetSkin));
        Material weapon = Material.matchMaterial(target.weapon());
        if (weapon != null) {
            session.viewer.getInventory().setItem(0, new ItemStack(weapon));
            session.viewer.getInventory().setHeldItemSlot(0);
        }
        String bar = Colors.translate(target.actionBar());
        session.tasks.add(plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (session.target != null) {
                ActionBar.send(session.viewer, bar);
            }
        }, 0L, TICKS_PER_SECOND));
    }

    private Optional<NpcSkin> skin(Player viewer) {
        List<Player> others = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        others.remove(viewer);
        Collections.shuffle(others);
        for (Player other : others) {
            Optional<NpcSkin> skin = PlayerSkins.of(other);
            if (skin.isPresent()) {
                return skin;
            }
        }
        return PlayerSkins.of(viewer);
    }

    private void hit(Player player, Npc npc) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.playback != null || !npc.id().equals(session.target)) {
            return;
        }
        targets.remove(session.target);
        targetNames.remove(session.target);
        session.target = null;
        ActionBar.send(player, "");
        player.playSound(npc.location(), Sound.HURT_FLESH, 1, 1);
        play(session, SceneCosmetics.onGround(session.stage.world(), npc.location()), SceneCosmetics.away(player.getLocation(), npc.location()), session.targetSkin, List.of());
    }

    private void play(Session session, Vec origin, Vec facing, Optional<NpcSkin> skin, List<UUID> riders) {
        Player viewer = session.viewer;
        if (!previewing(viewer) || session.animation.isEmpty()) {
            return;
        }
        onScene.accept(viewer);
        Scene scene = new Scene(origin, facing, skin.map(NpcSkin::value), riders, session.stage.settings().area(origin))
            .withBody(skin.map(found -> new SignedSkin(found.value(), found.signature())));
        session.playback = animator.play(session.animation.get(), session.stage.world(), scene, () -> List.of(viewer), () ->
            session.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(viewer, true), Math.max(1, session.stage.settings().returnDelay().toMillis() / 50))));
    }

    private void giveKit(Session session, PreviewKit kit) {
        session.kit = kit;
        kit.items().forEach((slot, item) -> session.viewer.getInventory().setItem(slot, item.build()));
        kit.items().keySet().stream().filter(slot -> slot < HOTBAR_SIZE).findFirst().ifPresent(session.viewer.getInventory()::setHeldItemSlot);
    }

    private void refill(Session session) {
        if (!previewing(session.viewer) || session.kit == null) {
            return;
        }
        PlayerInventory inventory = session.viewer.getInventory();
        session.kit.items().forEach((slot, item) -> {
            ItemStack current = inventory.getItem(slot);
            Material material = current == null ? Material.AIR : current.getType();
            int data = current == null ? 0 : current.getDurability();
            int amount = current == null ? 0 : current.getAmount();
            if (session.kit.needsRefill(slot, material, data, amount)) {
                inventory.setItem(slot, item.build());
            }
        });
    }

    private Optional<Session> kitSession(ProjectileSource shooter) {
        if (!(shooter instanceof Player player)) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(player.getUniqueId())).filter(session -> session.kit != null);
    }

    private ItemStack backItem(ScenePreviewSettings config) {
        Material material = Material.matchMaterial(config.back().material());
        return ItemBuilder.of(material == null ? Material.BED : material).name(config.back().name()).build();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        Action action = event.getAction();
        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        int held = event.getPlayer().getInventory().getHeldItemSlot();
        if (session.kit != null && session.stage.settings().usesKit(rightClick, held)) {
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }
        event.setCancelled(true);
        if (session.stage.settings().goesBack(rightClick, held)) {
            finish(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        kitSession(projectile.getShooter()).ifPresent(session -> {
            session.launched.add(projectile);
            kitPlay.ifPresent(play -> play.launched(session.viewer, session.cosmetic, projectile));
            session.tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> refill(session), session.kit.refillTicks()));
        });
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow && kitSession(arrow.getShooter()).isPresent()) {
            arrow.remove();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSplash(PotionSplashEvent event) {
        if (kitSession(event.getPotion().getShooter()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (previewing(event.getPlayer())) {
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
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player clicker && previewing(clicker)) {
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

    private void finish(Player viewer, boolean reopen) {
        Session session = sessions.remove(viewer.getUniqueId());
        if (session == null) {
            return;
        }
        session.cancel();
        session.launched.stream().filter(Projectile::isValid).forEach(Projectile::remove);
        kitPlay.ifPresent(play -> play.ended(viewer));
        if (session.target != null && targets != null) {
            targets.remove(session.target);
            targetNames.remove(session.target);
        }
        onLeave.accept(viewer);
        Title.clear(viewer);
        viewer.teleport(session.from);
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

    private record Stage(ScenePreviewSettings settings, World world) {
    }

    private static final class Session {

        private final Player viewer;
        private final Consumer<Player> back;
        private final Cosmetic cosmetic;
        private final Optional<SceneAnimation> animation;
        private final Stage stage;
        private final Location from;
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final GameMode mode;
        private final boolean allowFlight;
        private final Scoreboard scoreboard;
        private final List<Player> hidden = new ArrayList<>();
        private final List<BukkitTask> tasks = new ArrayList<>();
        private final List<Projectile> launched = new ArrayList<>();
        private BukkitTask timeout;
        private Playback playback;
        private String target;
        private Optional<NpcSkin> targetSkin = Optional.empty();
        private PreviewKit kit;

        Session(Player viewer, Consumer<Player> back, Cosmetic cosmetic, Optional<SceneAnimation> animation, Stage stage) {
            this.viewer = viewer;
            this.back = back;
            this.cosmetic = cosmetic;
            this.animation = animation;
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
            tasks.forEach(BukkitTask::cancel);
            if (timeout != null) {
                timeout.cancel();
            }
            if (playback != null) {
                playback.cancel();
            }
        }
    }
}
