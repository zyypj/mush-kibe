package br.com.muxe.minigame.api.bukkit;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.effect.SoundCue;
import br.com.muxe.core.bukkit.scoreboard.Sidebar;
import br.com.muxe.core.bukkit.text.CenteredText;
import br.com.muxe.core.bukkit.text.ChatLines;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.match.MatchReturns;
import br.com.muxe.minigame.api.runtime.Stage;
import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.api.spectator.Ghosts;
import br.com.muxe.minigame.api.view.EndTitles;
import br.com.muxe.minigame.engine.match.HealthDisplay;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.world.cell.BlockBox;
import br.com.muxe.minigame.engine.world.cell.Cell;
import br.com.muxe.minigame.engine.world.server.RegionEraser;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class BukkitStage implements Stage {

    public static final GameMode PLAYER_MODE = GameMode.SURVIVAL;
    public static final GameMode SPECTATOR_MODE = GameMode.ADVENTURE;

    private final MuxeCore core;
    private final World world;
    private final Cell cell;
    private final HealthDisplay health;
    private final EndTitles endTitles;
    private final MatchReturns returns;
    private final Logger logger;
    private final Map<UUID, Sidebar> sidebars = new HashMap<>();

    public BukkitStage(MuxeCore core, World world, Cell cell, HealthDisplay health, EndTitles endTitles, MatchReturns returns, Logger logger) {
        this.core = core;
        this.world = world;
        this.cell = cell;
        this.health = health;
        this.endTitles = endTitles;
        this.returns = returns;
        this.logger = logger;
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public Location location(Point point) {
        return new Location(world, cell.toWorldX(point.x()), point.y(), cell.toWorldZ(point.z()), point.yaw(), point.pitch());
    }

    @Override
    public Optional<Player> player(UUID id) {
        Player player = Bukkit.getPlayer(id);
        return player != null && player.getWorld().equals(world) ? Optional.of(player) : Optional.empty();
    }

    @Override
    public List<UUID> present(Collection<UUID> ids) {
        List<UUID> present = new ArrayList<>();
        for (UUID id : ids) {
            if (player(id).isPresent()) {
                present.add(id);
            }
        }
        return present;
    }

    @Override
    public String rankColored(UUID id, String name) {
        return Colors.translate(core.permissions().display(id).color()) + name;
    }

    @Override
    public void message(UUID id, String text) {
        online(id).ifPresent(player -> player.sendMessage(text));
    }

    @Override
    public void centered(UUID id, List<String> lines) {
        player(id).ifPresent(player -> ChatLines.send(player, lines, CenteredText.DEFAULT_CENTER, ""));
    }

    @Override
    public void title(UUID id, Title title) {
        online(id).ifPresent(title::send);
    }

    @Override
    public void sound(UUID id, Optional<SoundCue> cue) {
        cue.ifPresent(sound -> online(id).ifPresent(sound::play));
    }

    @Override
    public void prepare(UUID id, Look look) {
        online(id).ifPresent(player -> {
            reset(player, look == Look.PLAYER ? PLAYER_MODE : SPECTATOR_MODE);
            if (look == Look.SPECTATOR) {
                Ghosts.become(player);
            }
        });
    }

    @Override
    public void teleport(UUID id, Point point) {
        online(id).ifPresent(player -> player.teleport(location(point)));
    }

    @Override
    public void teleportOut(UUID id) {
        Location fallback = Bukkit.getWorlds().getFirst().getSpawnLocation();
        online(id).ifPresent(player -> player.teleport(fallback));
    }

    @Override
    public void knockUp(UUID id, double speed) {
        online(id).ifPresent(player -> player.setVelocity(new Vector(0, speed, 0)));
    }

    @Override
    public void raiseTo(UUID id, Point floor) {
        online(id).ifPresent(player -> {
            Location to = player.getLocation();
            to.setY(location(floor).getY());
            player.setFallDistance(0);
            player.teleport(to);
            player.setVelocity(new Vector());
        });
    }

    @Override
    public void waitingItems(UUID id, List<HotbarItem> items) {
        online(id).ifPresent(player -> items.forEach(item -> player.getInventory().setItem(item.slot(), named(item))));
    }

    @Override
    public void spectatorItems(UUID id, List<HotbarItem> items) {
        online(id).ifPresent(player -> Ghosts.give(player, items));
    }

    @Override
    public void showBoard(UUID id) {
        online(id).ifPresent(player -> sidebars.put(id, Sidebar.create(player)));
    }

    @Override
    public void board(UUID id, String title, List<String> lines) {
        Sidebar sidebar = sidebars.get(id);
        if (sidebar != null) {
            sidebar.title(title);
            sidebar.lines(lines);
        }
    }

    @Override
    public void dropBoard(UUID id) {
        sidebars.remove(id);
        online(id).ifPresent(player -> player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()));
    }

    @Override
    public void health(Collection<UUID> viewers, Collection<UUID> alive) {
        Map<String, Integer> points = new HashMap<>();
        for (UUID id : alive) {
            player(id).ifPresent(player -> points.put(player.getName(), HealthDisplay.points(player.getHealth())));
        }
        for (UUID viewer : viewers) {
            Sidebar sidebar = sidebars.get(viewer);
            if (sidebar != null) {
                health.show(sidebar.scoreboard(), points);
            }
        }
    }

    @Override
    public void refreshNameTag(UUID id) {
        online(id).ifPresent(player -> core.nameTags().refresh(player));
    }

    @Override
    public void visibility(Collection<UUID> online, Predicate<UUID> spectator) {
        List<Player> players = new ArrayList<>();
        online.forEach(id -> player(id).ifPresent(players::add));
        Ghosts.refresh(players, player -> spectator.test(player.getUniqueId()));
    }

    @Override
    public void sendTo(UUID id, String group) {
        core.network().connectToGroup(id, group);
    }

    @Override
    public void erase(Point first, Point second) {
        Location a = location(first);
        Location b = location(second);
        BlockBox box = BlockBox.of(a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ());
        RegionEraser.Result result = RegionEraser.erase(world, box);
        logger.fine("Cell " + cell.index() + " removed its waiting area: " + result);
    }

    @Override
    public void endTitle(UUID id, boolean won) {
        online(id).ifPresent(player -> {
            if (won) {
                endTitles.victory(player);
            } else {
                endTitles.defeat(player);
            }
        });
    }

    @Override
    public void rememberReturn(UUID id, Duration timeToLive) {
        String server = core.identity().id();
        CompletableFuture.runAsync(() -> returns.remember(id, server, timeToLive), core.async()).exceptionally(error -> {
            logger.log(Level.FINE, "Could not remember the match of " + id, error);
            return null;
        });
    }

    @Override
    public void forgetReturn(UUID id) {
        CompletableFuture.runAsync(() -> returns.forget(id), core.async()).exceptionally(error -> {
            logger.log(Level.FINE, "Could not forget the match of " + id, error);
            return null;
        });
    }

    private static Optional<Player> online(UUID id) {
        return Optional.ofNullable(Bukkit.getPlayer(id));
    }

    private static void reset(Player player, GameMode mode) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(mode);
        Ghosts.restore(player);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setExp(0);
        player.setLevel(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private static ItemStack named(HotbarItem entry) {
        Material material = Material.matchMaterial(entry.material());
        ItemStack item = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Colors.translate(entry.name()));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void knockback(UUID id, Optional<KnockbackProfile> profile) {
        profile.ifPresentOrElse(chosen -> core.knockback().assign(id, chosen), () -> core.knockback().clear(id));
    }
}
