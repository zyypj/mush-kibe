package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.core.bukkit.nms.EndermanView;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.bedwars.game.BridgeEgg;
import br.com.muxe.minigame.bedwars.game.Cell;
import br.com.muxe.minigame.bedwars.game.Cooldowns;
import br.com.muxe.minigame.bedwars.game.IceBridge;
import br.com.muxe.minigame.bedwars.game.Parachute;
import br.com.muxe.minigame.bedwars.game.Rewind;
import br.com.muxe.minigame.bedwars.game.SpiderWeb;
import br.com.muxe.minigame.bedwars.listener.LaunchFalls;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class SpecialItems {

    private final Plugin plugin;
    private final Clock clock;
    private final Match match;
    private final Function<UUID, Optional<Player>> players;
    private final Predicate<Location> mayPlace;
    private final Map<UUID, Rewind> paths = new HashMap<>();
    private final Map<UUID, Rewinding> rewinding = new HashMap<>();
    private final Cooldowns spiders = new Cooldowns(SpiderWeb.COOLDOWN);
    private final Cooldowns rewinds = new Cooldowns(Rewind.COOLDOWN);
    private final Cooldowns parachutes = new Cooldowns(Parachute.COOLDOWN);
    private final Map<Long, Material> temporary = new HashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    public SpecialItems(Plugin plugin, Clock clock, Match match, Function<UUID, Optional<Player>> players, Predicate<Location> mayPlace) {
        this.plugin = plugin;
        this.clock = clock;
        this.match = match;
        this.players = players;
        this.mayPlace = mayPlace;
    }

    public void start() {
        repeat(() -> {
            for (Participant participant : match.players()) {
                if (participant.isAlive() && !rewinding.containsKey(participant.id())) {
                    players.apply(participant.id()).ifPresent(player -> {
                        Location at = player.getLocation();
                        paths.computeIfAbsent(player.getUniqueId(), id -> new Rewind())
                            .record(new Rewind.Sample(at.getX(), at.getY(), at.getZ(), at.getYaw(), at.getPitch()));
                    });
                }
            }
            return match.phase() == MatchPhase.PLAYING;
        });
    }

    public void close() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        for (UUID id : new ArrayList<>(rewinding.keySet())) {
            players.apply(id).ifPresent(player -> stopRewinding(player, true));
        }
        rewinding.clear();
        paths.clear();
    }

    public void iceBridge(Player player) {
        Location from = player.getLocation();
        World world = from.getWorld();
        int[] tick = {0};
        repeat(() -> {
            tick[0]++;
            List<Cell> placed = new ArrayList<>();
            for (Cell cell : IceBridge.at(tick[0], from.getX(), from.getY(), from.getZ(), from.getYaw())) {
                if (place(world, cell, Material.ICE, 0, true)) {
                    placed.add(cell);
                    world.playSound(center(world, cell), Sound.GLASS, 1f, 1f);
                }
            }
            if (!placed.isEmpty()) {
                later(IceBridge.MELT_TICKS, () -> placed.forEach(cell -> {
                    if (clear(world, cell)) {
                        world.playSound(center(world, cell), Sound.CHICKEN_EGG_POP, 1f, 1f);
                    }
                }));
            }
            return tick[0] < IceBridge.LAST_TICK;
        });
    }

    public void bridgeEgg(Egg egg, TeamColor team) {
        Location from = egg.getLocation();
        World world = from.getWorld();
        int[] tick = {0};
        repeat(() -> {
            if (!egg.isValid()) {
                return false;
            }
            Location at = egg.getLocation();
            if (!BridgeEgg.builds(from.getX(), from.getY(), from.getZ(), at.getX(), at.getY(), at.getZ(), ++tick[0])) {
                return false;
            }
            boolean any = false;
            for (Cell cell : BridgeEgg.under(at.getX(), at.getY(), at.getZ())) {
                any |= place(world, cell, Material.WOOL, team.woolData(), false);
            }
            if (any) {
                world.playSound(at.clone().subtract(0, BridgeEgg.DROP, 0), Sound.CHICKEN_EGG_POP, BridgeEgg.SOUND_VOLUME, BridgeEgg.SOUND_PITCH);
            }
            return true;
        });
    }

    public Optional<Duration> spiderCooldown(UUID player) {
        return spiders.remaining(player, clock.instant());
    }

    public void shootSpider(Player player) {
        spiders.start(player.getUniqueId(), clock.instant());
        Snowball shot = player.launchProjectile(Snowball.class);
        shot.setMetadata(SpiderWeb.SNOWBALL_TAG, new FixedMetadataValue(plugin, true));
        player.getWorld().playSound(player.getLocation(), Sound.SHOOT_ARROW, 1f, SpiderWeb.SHOT_PITCH);
    }

    public void spiderLanded(Location at) {
        World world = at.getWorld();
        List<Cell> webs = new ArrayList<>();
        for (Cell cell : SpiderWeb.webs(at.getX(), at.getY(), at.getZ())) {
            if (place(world, cell, Material.WEB, 0, true)) {
                webs.add(cell);
            }
        }
        if (webs.isEmpty()) {
            return;
        }
        world.playSound(at, Sound.SPIDER_IDLE, 1f, SpiderWeb.WEB_PITCH);
        later(SpiderWeb.WEB_TICKS, () -> {
            boolean any = false;
            for (Cell cell : webs) {
                any |= clear(world, cell);
            }
            if (any) {
                world.playSound(at, Sound.SPIDER_DEATH, 1f, SpiderWeb.WEB_PITCH);
            }
        });
    }

    public Optional<Duration> rewindCooldown(UUID player) {
        return rewinds.remaining(player, clock.instant());
    }

    public boolean rewind(Player player) {
        UUID id = player.getUniqueId();
        Rewind path = paths.get(id);
        List<Rewind.Sample> trail = path == null ? List.of() : path.trail();
        if (trail.isEmpty() || rewinding.containsKey(id)) {
            return false;
        }
        path.clear();
        rewinds.start(id, clock.instant());
        World world = player.getWorld();
        boolean couldFly = player.getAllowFlight();
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFallDistance(0);
        rewinding.put(id, new Rewinding(EndermanView.open(player, eye(player.getLocation())), couldFly));
        world.playSound(player.getLocation(), Sound.PORTAL_TRAVEL, 1f, Rewind.SOUND_PITCH);
        int[] step = {0};
        repeat(() -> {
            if (!rewinding.containsKey(id)) {
                return false;
            }
            boolean alive = match.player(id).filter(Participant::isAlive).isPresent();
            if (!player.isOnline() || !alive || !player.getWorld().equals(world)) {
                stopRewinding(player, false);
                return false;
            }
            if (step[0] < trail.size()) {
                Rewind.Sample sample = trail.get(step[0]);
                Location to = new Location(world, sample.x(), sample.y(), sample.z(), sample.yaw(), sample.pitch());
                player.teleport(to);
                player.setFallDistance(0);
                rewinding.get(id).view().move(eye(to));
            } else if (step[0] >= trail.size() + Rewind.HOLD_TICKS) {
                stopRewinding(player, true);
                return false;
            }
            step[0]++;
            return true;
        });
        return true;
    }

    public boolean isRewinding(UUID player) {
        return rewinding.containsKey(player);
    }

    public Optional<Duration> parachuteCooldown(UUID player) {
        return parachutes.remaining(player, clock.instant());
    }

    @SuppressWarnings("deprecation")
    public void parachute(Player player, TeamColor team) {
        parachutes.start(player.getUniqueId(), clock.instant());
        int platformY = Parachute.platformY(player.getLocation().getY());
        World world = player.getWorld();
        int[] tick = {0};
        repeat(() -> {
            boolean alive = match.player(player.getUniqueId()).filter(Participant::isAlive).isPresent();
            if (!player.isOnline() || !alive || !player.getWorld().equals(world) || ++tick[0] > Parachute.WATCH_TICKS || player.isOnGround()) {
                return false;
            }
            Location at = player.getLocation();
            if (!Parachute.reached(at.getY(), platformY)) {
                return true;
            }
            openParachute(player, world, at, platformY, team);
            return false;
        });
    }

    public boolean isTemporary(Block block) {
        return temporary.containsKey(new Cell(block.getX(), block.getY(), block.getZ()).key());
    }

    public void forgetTemporary(Block block) {
        temporary.remove(new Cell(block.getX(), block.getY(), block.getZ()).key());
    }

    @SuppressWarnings("deprecation")
    private void openParachute(Player player, World world, Location at, int platformY, TeamColor team) {
        List<Cell> cells = Parachute.platform(at.getX(), platformY, at.getZ());
        if (!place(world, cells.getFirst(), Material.WOOL, team.woolData(), true)) {
            return;
        }
        Set<Long> platform = new HashSet<>();
        platform.add(cells.getFirst().key());
        List<Cell> placed = new ArrayList<>(List.of(cells.getFirst()));
        for (Cell cell : cells.subList(1, cells.size())) {
            if (place(world, cell, Material.STAINED_GLASS, team.woolData(), true)) {
                placed.add(cell);
                platform.add(cell.key());
            }
        }
        Location top = new Location(world, at.getX(), platformY + 1, at.getZ(), at.getYaw(), at.getPitch());
        player.teleport(top);
        player.setFallDistance(0);
        launch(player);
        world.playSound(center(world, cells.getFirst()), Sound.ANVIL_LAND, Parachute.SOUND_VOLUME, Parachute.LAUNCH_PITCH);
        int[] tick = {0};
        repeat(() -> {
            if (++tick[0] >= Parachute.PLATFORM_TICKS) {
                boolean any = false;
                for (Cell cell : placed) {
                    any |= clear(world, cell);
                }
                if (any) {
                    world.playSound(center(world, cells.getFirst()), Sound.DIG_STONE, Parachute.SOUND_VOLUME, Parachute.BREAK_PITCH);
                }
                return false;
            }
            if (player.isOnline() && player.isOnGround() && player.getWorld().equals(world)) {
                Location feet = player.getLocation();
                if (platform.contains(Cell.of(feet.getX(), feet.getY() - 0.5, feet.getZ()).key())) {
                    player.setFallDistance(0);
                    launch(player);
                }
            }
            return true;
        });
    }

    private void launch(Player player) {
        player.setVelocity(new Vector(0, Parachute.LAUNCH, 0));
        LaunchFalls.mark(plugin, player, clock.instant());
        player.getWorld().playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, Parachute.SOUND_VOLUME, Parachute.LAUNCH_PITCH);
    }

    private void stopRewinding(Player player, boolean restoreFlight) {
        Rewinding state = rewinding.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        state.view().close();
        if (restoreFlight && player.isOnline()) {
            player.setFlying(false);
            player.setAllowFlight(state.couldFly());
            player.setFallDistance(0);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean place(World world, Cell cell, Material material, int data, boolean temporary) {
        Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
        if (block.getType() != Material.AIR || !mayPlace.test(block.getLocation())) {
            return false;
        }
        block.setTypeIdAndData(material.getId(), (byte) data, false);
        match.placed(cell.x(), cell.y(), cell.z());
        if (temporary) {
            this.temporary.put(cell.key(), material);
        }
        return true;
    }

    private boolean clear(World world, Cell cell) {
        Material expected = temporary.remove(cell.key());
        Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
        if (expected == null || block.getType() != expected) {
            return false;
        }
        block.setType(Material.AIR, false);
        match.unplaced(cell.x(), cell.y(), cell.z());
        return true;
    }

    private void repeat(BooleanSupplier step) {
        BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!step.getAsBoolean()) {
                task[0].cancel();
                tasks.remove(task[0]);
            }
        }, 1L, 1L);
        tasks.add(task[0]);
    }

    private void later(long ticks, Runnable action) {
        BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tasks.remove(task[0]);
            action.run();
        }, ticks);
        tasks.add(task[0]);
    }

    private static Location center(World world, Cell cell) {
        return new Location(world, cell.x() + 0.5, cell.y() + 0.5, cell.z() + 0.5);
    }

    private static Location eye(Location feet) {
        return feet.clone().subtract(0, Rewind.CAMERA_DROP, 0);
    }

    private record Rewinding(EndermanView view, boolean couldFly) {
    }
}
