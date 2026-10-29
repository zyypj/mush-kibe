package br.com.muxe.core.bukkit.parkour;

import br.com.muxe.core.bukkit.display.Display;
import br.com.muxe.core.bukkit.display.DisplayService;
import br.com.muxe.core.bukkit.effect.FireworkBurst;
import br.com.muxe.core.bukkit.placement.Reconciler;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.cooldown.CooldownFormat;
import br.com.muxe.core.common.cooldown.SpamDecision;
import br.com.muxe.core.common.cooldown.SpamGuard;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.parkour.application.command.SubmitParkourTimeCommand;
import br.com.muxe.core.common.parkour.application.query.FindParkourRecordQuery;
import br.com.muxe.core.common.parkour.domain.ParkourCourse;
import br.com.muxe.core.common.parkour.domain.ParkourOutcome;
import br.com.muxe.core.common.parkour.domain.ParkourRuns;
import br.com.muxe.core.common.parkour.domain.ParkourStep;
import br.com.muxe.core.common.parkour.domain.ParkourTime;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.Plugin;

public final class Parkour implements Listener {

    public static final String DISPLAY_PREFIX = "parkour:";

    private static final double CENTER = 0.5;

    private final Plugin plugin;
    private final ParkourSettings settings;
    private final String group;
    private final CommandBus commands;
    private final QueryBus queries;
    private final Executor mainThread;
    private final MessageCatalog messages;
    private final SpamGuard spam;
    private final ParkourRuns runs;
    private final DisplayService displays;
    private final Reconciler<ParkourPointPlacement> shown = new Reconciler<>(ParkourPointPlacement::id);
    private final Map<UUID, ParkourGear> gear = new ConcurrentHashMap<>();
    private final Map<UUID, Optional<Duration>> records = new ConcurrentHashMap<>();
    private volatile Optional<ParkourCourse> course = Optional.empty();

    public Parkour(
        Plugin plugin,
        ParkourSettings settings,
        String group,
        CommandBus commands,
        QueryBus queries,
        Executor mainThread,
        MessageCatalog messages,
        SpamGuard spam,
        Clock clock
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.group = group;
        this.commands = commands;
        this.queries = queries;
        this.mainThread = mainThread;
        this.messages = messages;
        this.spam = spam;
        this.runs = new ParkourRuns(clock);
        this.displays = DisplayService.start(plugin);
        this.displays.labelText((viewer, text) -> text.replace(ParkourSettings.RECORD, recordText(viewer.getUniqueId())));
    }

    public void show(List<ParkourPointPlacement> points) {
        course = ParkourCourse.from(points);
        Reconciler.Changes<ParkourPointPlacement> changes = shown.next(points);
        changes.removed().forEach(id -> displays.remove(DISPLAY_PREFIX + id));
        for (ParkourPointPlacement point : changes.put()) {
            Optional<Location> plate = plate(point.position());
            if (plate.isEmpty()) {
                displays.remove(DISPLAY_PREFIX + point.id());
                plugin.getLogger().warning("Parkour point '" + point.id() + "' is in world '" + point.position().world() + "', which is not loaded");
                continue;
            }
            displays.put(new Display(DISPLAY_PREFIX + point.id(), plate.get(), List.of(), settings.labelsFor(point.point())));
        }
    }

    public boolean isRunning(Player player) {
        return runs.isRunning(player.getUniqueId());
    }

    public void stop() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            leave(player);
        }
        displays.stop();
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || to.getWorld() == null || sameBlock(from, to)) {
            return;
        }
        Optional<ParkourCourse> current = course;
        if (current.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        current.get().at(to.getWorld().getName(), to.getBlockX(), to.getBlockY(), to.getBlockZ())
            .ifPresent(point -> handle(player, point, runs.stepOn(player.getUniqueId(), current.get(), point.point())));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!runs.isRunning(player.getUniqueId())) {
            return;
        }
        Optional<ParkourItem> item = settings.itemAt(player.getInventory().getHeldItemSlot(), event.getItem());
        if (item.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        SpamDecision decision = spam.use(player.getUniqueId());
        if (!decision.allowed()) {
            player.sendMessage(messages.get("spam-cooldown", "seconds", CooldownFormat.seconds(decision.remaining())));
            return;
        }
        Optional<ParkourCourse> current = course;
        if (current.isEmpty() || item.get() == ParkourItem.STOP) {
            leave(player);
            player.sendMessage(messages.get("parkour-stopped"));
            return;
        }
        ParkourStep step = item.get() == ParkourItem.RESTART
            ? runs.restart(player.getUniqueId(), current.get())
            : runs.backToCheckpoint(player.getUniqueId(), current.get());
        handle(player, current.get().start(), step);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        keepGrounded(event, runs.isRunning(event.getPlayer().getUniqueId()));
    }

    static void keepGrounded(PlayerToggleFlightEvent event, boolean running) {
        if (running && event.isFlying()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        queries.ask(new FindParkourRecordQuery(group, player.getUniqueId())).whenCompleteAsync((best, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Could not load the parkour record of " + player.getName(), error);
                return;
            }
            if (player.isOnline()) {
                records.put(player.getUniqueId(), best);
                displays.refresh(player);
            }
        }, mainThread);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        runs.stop(id);
        gear.remove(id);
        records.remove(id);
    }

    private void handle(Player player, ParkourPointPlacement point, ParkourStep step) {
        switch (step) {
            case ParkourStep.Started _ -> {
                gear.put(player.getUniqueId(), ParkourGear.equip(player, settings.buildItems()));
                player.sendMessage(messages.get("parkour-started"));
            }
            case ParkourStep.Restarted restarted -> {
                restarted.teleport().ifPresent(position -> teleport(player, position));
                player.sendMessage(messages.get("parkour-restarted"));
            }
            case ParkourStep.CheckpointReached reached -> player.sendMessage(messages.get("parkour-checkpoint",
                "number", Integer.toString(reached.number()), "time", ParkourTime.format(reached.elapsed())));
            case ParkourStep.ReturnedToCheckpoint returned -> {
                teleport(player, returned.position());
                player.sendMessage(messages.get("parkour-returned", "number", Integer.toString(returned.number())));
            }
            case ParkourStep.Finished finished -> finish(player, point, finished.elapsed());
            case ParkourStep.Ignored _ -> {
            }
        }
    }

    private void finish(Player player, ParkourPointPlacement end, Duration time) {
        restoreGear(player);
        player.sendMessage(messages.get("parkour-finished", "time", ParkourTime.format(time)));
        plate(end.position()).ifPresent(at -> FireworkBurst.explode(plugin, at, settings.finishFirework()));
        commands.dispatch(new SubmitParkourTimeCommand(group, player.getUniqueId(), time)).whenCompleteAsync((result, error) -> {
            if (error != null || result.isFailure()) {
                plugin.getLogger().log(Level.WARNING, "Could not save the parkour time of " + player.getName(), error);
                return;
            }
            recorded(player, result);
        }, mainThread);
    }

    private void recorded(Player player, Result<ParkourOutcome> result) {
        ParkourOutcome outcome = result.orElseThrow();
        if (!player.isOnline()) {
            return;
        }
        records.put(player.getUniqueId(), Optional.of(outcome.best()));
        displays.refresh(player);
        if (outcome.newRecord() && outcome.previous().isPresent()) {
            player.sendMessage(messages.get("parkour-new-record",
                "previous", ParkourTime.format(outcome.previous().get()), "time", ParkourTime.format(outcome.time())));
        }
    }

    private void leave(Player player) {
        if (runs.stop(player.getUniqueId())) {
            restoreGear(player);
        }
    }

    private void restoreGear(Player player) {
        ParkourGear saved = gear.remove(player.getUniqueId());
        if (saved != null) {
            saved.restore(player);
        }
    }

    private void teleport(Player player, Position position) {
        plate(position).ifPresent(at -> {
            at.setYaw(position.yaw());
            at.setPitch(position.pitch());
            player.setFallDistance(0);
            player.teleport(at);
        });
    }

    private String recordText(UUID player) {
        return records.getOrDefault(player, Optional.empty()).map(ParkourTime::format).orElse(settings.noRecord());
    }

    private Optional<Location> plate(Position position) {
        World world = plugin.getServer().getWorld(position.world());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, Math.floor(position.x()) + CENTER, Math.floor(position.y()), Math.floor(position.z()) + CENTER));
    }

    private static boolean sameBlock(Location from, Location to) {
        return from.getWorld() == to.getWorld()
            && from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ();
    }
}
