package br.com.muxe.minigame.engine.setup.session;

import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.ChatButton;
import br.com.muxe.core.bukkit.text.ClickableText;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.ArenaDraft.Change;
import br.com.muxe.minigame.engine.setup.ArenaFiles;
import br.com.muxe.minigame.engine.setup.ArenaFiles.StoredArena;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.setup.SetupChecklist;
import br.com.muxe.minigame.engine.setup.SetupSchema;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.setup.StepKind;
import br.com.muxe.minigame.engine.setup.TeamRule;
import br.com.muxe.minigame.engine.setup.session.SetupMarkers.Marker;
import br.com.muxe.minigame.engine.setup.session.SetupSession.Return;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.anvil.AnvilImporter;
import br.com.muxe.minigame.engine.world.map.EditableMap;
import br.com.muxe.minigame.engine.world.map.MapFiles;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import br.com.muxe.minigame.engine.world.server.MuxeWorlds;
import br.com.muxe.minigame.engine.world.server.VoidGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class SetupService implements Listener {

    public static final String PERMISSION = "muxe.minigame.setup";
    public static final String WORLD_PREFIX = "setup_";

    private static final int TARGET_RANGE = 6;
    private static final int MARKER_RESEND_SECONDS = 3;

    private final Plugin plugin;
    private final SetupSchema schema;
    private final MessageCatalog messages;
    private final MuxeWorlds worlds;
    private final ArenaFiles arenas;
    private final Path maps;
    private final SetupHelp help;
    private final SetupMarkers markers = new SetupMarkers(TextHolograms.standard());
    private final Map<String, SetupSession> sessions = new LinkedHashMap<>();
    private final Map<UUID, String> editing = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final ExecutorService disk = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("muxe-setup-disk").factory());
    private BukkitTask ticker;
    private int ticks;

    public SetupService(Plugin plugin, SetupSchema schema, MessageCatalog messages, MuxeWorlds worlds, Path dataFolder) {
        this.plugin = plugin;
        this.schema = schema;
        this.messages = messages;
        this.worlds = worlds;
        this.arenas = new ArenaFiles(dataFolder.resolve("arenas"));
        this.maps = dataFolder.resolve("maps");
        this.help = new SetupHelp(messages);
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (ticker != null) {
            ticker.cancel();
        }
        for (SetupSession session : List.copyOf(sessions.values())) {
            for (UUID editor : List.copyOf(session.editors())) {
                Player player = plugin.getServer().getPlayer(editor);
                restore(player, session.leave(editor));
                editing.remove(editor);
            }
            close(session);
        }
        disk.close();
    }

    public SetupSchema schema() {
        return schema;
    }

    public ArenaFiles arenas() {
        return arenas;
    }

    public Path mapFile(String name) {
        return maps.resolve(name + MapFiles.EXTENSION);
    }

    public Optional<SetupSession> sessionOf(Player player) {
        return Optional.ofNullable(editing.get(player.getUniqueId())).map(sessions::get);
    }

    public void open(Player player, String rawName, Optional<String> rawMode) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (!ArenaDraft.isValidName(name)) {
            player.sendMessage(messages.get("setup-invalid-name"));
            return;
        }
        SetupSession existing = sessions.get(name);
        if (existing != null) {
            join(player, existing);
            return;
        }
        if (!loading.add(name)) {
            player.sendMessage(messages.get("setup-loading", "arena", name));
            return;
        }
        player.sendMessage(messages.get("setup-loading", "arena", name));
        Path folder = plugin.getServer().getWorldContainer().toPath().resolve(name);
        disk.execute(() -> {
            try {
                Optional<StoredArena> stored = arenas.read(name);
                Optional<String> mode = stored.map(arena -> arena.draft().mode())
                    .or(() -> rawMode.map(value -> value.toLowerCase(Locale.ROOT)))
                    .or(() -> schema.modeFromName(name))
                    .filter(schema.modes()::contains);
                if (mode.isEmpty()) {
                    main(() -> {
                        loading.remove(name);
                        player.sendMessage(messages.get("setup-unknown-mode", "arena", name, "modes", String.join(", ", schema.modes())));
                    });
                    return;
                }
                ArenaDraft draft = stored.map(StoredArena::draft).orElseGet(() -> ArenaDraft.create(name, mode.get(), schema));
                Path mapFile = mapFile(name);
                MapTemplate template;
                String origin;
                if (Files.isRegularFile(mapFile)) {
                    template = MapFiles.read(mapFile);
                    origin = "setup-loaded";
                } else if (AnvilImporter.isWorldFolder(folder)) {
                    template = AnvilImporter.importWorld(folder);
                    MapFiles.write(mapFile, template);
                    origin = "setup-converted";
                } else {
                    template = MapTemplate.empty();
                    origin = "setup-created-empty";
                }
                if (stored.isEmpty()) {
                    arenas.write(draft, false);
                }
                main(() -> finishOpening(player, draft, template, origin));
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Could not open arena " + name, exception);
                main(() -> {
                    loading.remove(name);
                    player.sendMessage(messages.get("setup-load-failed", "error", String.valueOf(exception.getMessage())));
                });
            }
        });
    }

    public void help(Player player) {
        sessionOf(player).ifPresentOrElse(session -> {
            SetupChecklist checklist = SetupChecklist.of(schema, session.draft());
            for (List<ChatButton> line : help.lines(session.draft(), checklist)) {
                ClickableText.send(player, line);
            }
        }, () -> messages.lines("setup-usage").forEach(player::sendMessage));
    }

    public void define(Player player, String stepId, Optional<String> teamId) {
        withStep(player, stepId, teamId, (session, step, team) -> {
            Optional<Point> point = pointFor(player, step);
            if (point.isEmpty()) {
                player.sendMessage(messages.get("setup-no-block"));
                return;
            }
            Change change = session.draft().set(step, team, point.get());
            String label = help.label(step, team);
            String position = point.get().describe();
            if (step.kind() == StepKind.HEIGHT) {
                player.sendMessage(messages.get("setup-height-set", "label", label, "height", String.valueOf(point.get().blockY())));
            } else if (step.kind() == StepKind.REGION) {
                player.sendMessage(messages.get("setup-region-corner", "label", label, "index", String.valueOf(change.index()), "position", position));
            } else {
                player.sendMessage(switch (change.type()) {
                    case SET -> messages.get("setup-point-set", "label", label, "position", position);
                    case REPLACED -> messages.get("setup-point-replaced", "label", label, "position", position);
                    case ADDED -> messages.get("setup-point-added", "label", label, "index", String.valueOf(change.index()), "position", position);
                    case FULL -> messages.get("setup-point-full", "label", label, "count", String.valueOf(change.count()));
                });
            }
            changed(session);
        });
    }

    public void remove(Player player, String stepId, Optional<String> teamId) {
        withStep(player, stepId, teamId, (session, step, team) -> {
            String label = help.label(step, team);
            if (session.draft().remove(step, team, Points.of(player.getLocation()))) {
                player.sendMessage(messages.get("setup-removed", "label", label));
                changed(session);
            } else {
                player.sendMessage(messages.get("setup-nothing-to-remove", "label", label));
            }
        });
    }

    public void team(Player player, boolean add, String colorId) {
        Optional<SetupSession> found = editingSession(player);
        if (found.isEmpty()) {
            return;
        }
        SetupSession session = found.get();
        ArenaDraft draft = session.draft();
        TeamRule rule = schema.teams(draft.mode());
        if (!rule.isChosen()) {
            player.sendMessage(messages.get("setup-team-fixed", "mode", draft.mode()));
            return;
        }
        Optional<TeamColor> color = TeamColor.find(colorId);
        if (color.isEmpty()) {
            player.sendMessage(messages.get("setup-unknown-team", "team", colorId, "teams", allColors()));
            return;
        }
        String colored = Colors.translate(color.get().colored());
        boolean done = add ? draft.addTeam(color.get(), rule) : draft.removeTeam(color.get(), rule);
        if (!done) {
            player.sendMessage(messages.get("setup-team-not-changed", "team", colored, "count", String.valueOf(draft.teams().size()), "total", String.valueOf(rule.count())));
            return;
        }
        player.sendMessage(add
            ? messages.get("setup-team-added", "team", colored, "count", String.valueOf(draft.teams().size()), "total", String.valueOf(rule.count()))
            : messages.get("setup-team-removed", "team", colored));
        changed(session);
    }

    public void rename(Player player, String displayName) {
        editingSession(player).ifPresent(session -> {
            session.draft().rename(Colors.strip(Colors.translate(displayName)).trim());
            player.sendMessage(messages.get("setup-renamed", "name", session.draft().displayName()));
            changed(session);
        });
    }

    public void save(Player player) {
        Optional<SetupSession> found = editingSession(player);
        if (found.isEmpty()) {
            return;
        }
        SetupSession session = found.get();
        ArenaDraft draft = session.draft();
        player.sendMessage(messages.get("setup-saving", "arena", draft.name()));
        session.world().save();
        MapTemplate snapshot = session.map().snapshot();
        SetupChecklist checklist = SetupChecklist.of(schema, draft);
        String yaml = ArenaFiles.render(draft, checklist.isReady());
        disk.execute(() -> {
            try {
                MapFiles.write(mapFile(draft.name()), snapshot);
                arenas.writeRendered(draft.name(), yaml);
                main(() -> {
                    if (checklist.isReady()) {
                        player.sendMessage(messages.get("setup-saved-ready", "arena", draft.name()));
                    } else if (checklist.missingCount() == 1) {
                        player.sendMessage(messages.get("setup-saved-missing-one", "arena", draft.name()));
                    } else {
                        player.sendMessage(messages.get("setup-saved-missing", "arena", draft.name(), "missing", String.valueOf(checklist.missingCount())));
                    }
                });
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Could not save arena " + draft.name(), exception);
                main(() -> player.sendMessage(messages.get("setup-save-failed", "error", String.valueOf(exception.getMessage()))));
            }
        });
    }

    public void leave(Player player) {
        String name = editing.remove(player.getUniqueId());
        if (name == null) {
            player.sendMessage(messages.get("setup-not-editing"));
            return;
        }
        SetupSession session = sessions.get(name);
        markers.hide(player);
        restore(player, session.leave(player.getUniqueId()));
        player.sendMessage(messages.get("setup-left", "arena", name));
        if (session.isEmpty()) {
            close(session);
        }
    }

    public void list(Player player) {
        disk.execute(() -> {
            List<List<ChatButton>> lines = new ArrayList<>();
            try {
                for (String name : arenas.names()) {
                    Optional<StoredArena> arena = arenas.read(name);
                    if (arena.isEmpty()) {
                        continue;
                    }
                    String status = arena.get().ready() ? "setup-list-ready" : "setup-list-missing";
                    lines.add(List.of(
                        ChatButton.text(messages.get(status, "arena", name, "mode", arena.get().draft().mode())),
                        ChatButton.run(messages.get("setup-list-edit"), SetupHelp.COMMAND + " criar " + name, messages.get("setup-list-edit-hover", "arena", name))
                    ));
                }
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Could not list arenas", exception);
            }
            main(() -> {
                if (lines.isEmpty()) {
                    player.sendMessage(messages.get("setup-list-empty"));
                    return;
                }
                player.sendMessage(messages.get("setup-list-header", "count", String.valueOf(lines.size())));
                lines.forEach(line -> ClickableText.send(player, line));
            });
        });
    }

    public List<String> stepIds(Player player) {
        return sessionOf(player).map(session -> schema.steps(session.draft().mode()).stream().map(SetupStep::id).toList()).orElse(List.of());
    }

    public List<String> teamIds(Player player) {
        return sessionOf(player).map(session -> session.draft().teams().stream().map(TeamColor::id).toList()).orElse(List.of());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = editing.remove(player.getUniqueId());
        if (name == null) {
            return;
        }
        SetupSession session = sessions.get(name);
        markers.forget(player.getUniqueId());
        restore(player, session.leave(player.getUniqueId()));
        if (session.isEmpty()) {
            close(session);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String name = editing.get(player.getUniqueId());
        if (name == null || player.getWorld().equals(sessions.get(name).world())) {
            return;
        }
        editing.remove(player.getUniqueId());
        SetupSession session = sessions.get(name);
        markers.forget(player.getUniqueId());
        Return previous = session.leave(player.getUniqueId());
        if (previous != null) {
            player.setGameMode(previous.gameMode());
            player.setAllowFlight(previous.allowFlight());
        }
        if (session.isEmpty()) {
            close(session);
        }
    }

    private void finishOpening(Player player, ArenaDraft draft, MapTemplate template, String origin) {
        loading.remove(draft.name());
        EditableMap map = new EditableMap(template);
        World world;
        try {
            world = worlds.create(WORLD_PREFIX + draft.name(), map, true);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not create the setup world of " + draft.name(), exception);
            player.sendMessage(messages.get("setup-load-failed", "error", String.valueOf(exception.getMessage())));
            return;
        }
        SetupSession session = new SetupSession(draft, map, world);
        sessions.put(draft.name(), session);
        if (player.isOnline()) {
            player.sendMessage(messages.get(origin, "arena", draft.name(), "chunks", String.valueOf(template.chunkCount()), "size", size(template.compressedBytes())));
            join(player, session);
        } else if (session.isEmpty()) {
            close(session);
        }
    }

    private void join(Player player, SetupSession session) {
        String current = editing.get(player.getUniqueId());
        if (current != null && !current.equals(session.draft().name())) {
            leave(player);
        }
        if (!session.editors().contains(player.getUniqueId())) {
            session.enter(player.getUniqueId(), new Return(player.getLocation(), player.getGameMode(), player.getAllowFlight()));
        }
        editing.put(player.getUniqueId(), session.draft().name());
        player.teleport(start(session));
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(messages.get("setup-opened", "arena", session.draft().name(), "mode", session.draft().mode()));
        help(player);
        refresh(session);
    }

    private Location start(SetupSession session) {
        World world = session.world();
        int highest = world.getHighestBlockYAt(0, 0);
        double y = highest > 0 ? highest + 1 : VoidGenerator.SPAWN_Y;
        return new Location(world, 0.5, y, 0.5);
    }

    private void restore(Player player, Return previous) {
        if (player == null || previous == null) {
            return;
        }
        Location back = previous.location();
        if (back.getWorld() == null || plugin.getServer().getWorld(back.getWorld().getName()) == null || MuxeWorlds.isOurs(back.getWorld().getName())) {
            back = plugin.getServer().getWorlds().getFirst().getSpawnLocation();
        }
        player.teleport(back);
        player.setGameMode(previous.gameMode());
        player.setAllowFlight(previous.allowFlight());
    }

    private void close(SetupSession session) {
        sessions.remove(session.draft().name());
        Location fallback = plugin.getServer().getWorlds().getFirst().getSpawnLocation();
        for (Player inside : session.world().getPlayers()) {
            inside.teleport(fallback);
        }
        worlds.destroy(session.world());
    }

    private void changed(SetupSession session) {
        String name = session.draft().name();
        String yaml = ArenaFiles.render(session.draft(), SetupChecklist.of(schema, session.draft()).isReady());
        disk.execute(() -> {
            try {
                arenas.writeRendered(name, yaml);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Could not write arena " + name, exception);
            }
        });
        refresh(session);
    }

    private void refresh(SetupSession session) {
        List<Marker> list = SetupMarkers.markers(schema, session.draft(), help);
        for (UUID editor : session.editors()) {
            Player player = plugin.getServer().getPlayer(editor);
            if (player != null && player.getWorld().equals(session.world())) {
                markers.show(player, session.world(), list);
            }
        }
    }

    private void tick() {
        ticks++;
        for (SetupSession session : sessions.values()) {
            String bar = help.actionBar(SetupChecklist.of(schema, session.draft()));
            for (UUID editor : session.editors()) {
                Player player = plugin.getServer().getPlayer(editor);
                if (player == null || !player.getWorld().equals(session.world())) {
                    continue;
                }
                ActionBar.send(player, bar);
                if (ticks % MARKER_RESEND_SECONDS == 0) {
                    markers.resend(player);
                }
            }
        }
    }

    private Optional<Point> pointFor(Player player, SetupStep step) {
        Location location = player.getLocation();
        return switch (step.kind()) {
            case LOCATION -> Optional.of(Points.of(location).standing());
            case CENTER, REGION -> Optional.of(Point.block(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            case HEIGHT -> Optional.of(Points.of(location));
            case BLOCK -> {
                Block target = player.getTargetBlock((Set<Material>) null, TARGET_RANGE);
                yield target == null || target.getType() == Material.AIR
                    ? Optional.empty()
                    : Optional.of(Point.block(target.getX(), target.getY(), target.getZ()));
            }
        };
    }

    private void withStep(Player player, String stepId, Optional<String> teamId, StepAction action) {
        Optional<SetupSession> found = editingSession(player);
        if (found.isEmpty()) {
            return;
        }
        SetupSession session = found.get();
        Optional<SetupStep> step = schema.step(session.draft().mode(), stepId.toLowerCase(Locale.ROOT));
        if (step.isEmpty()) {
            player.sendMessage(messages.get("setup-unknown-step", "step", stepId));
            return;
        }
        TeamColor team = null;
        if (step.get().perTeam()) {
            if (teamId.isEmpty()) {
                player.sendMessage(messages.get("setup-needs-team", "step", step.get().id()));
                return;
            }
            Optional<TeamColor> color = TeamColor.find(teamId.get()).filter(session.draft().teams()::contains);
            if (color.isEmpty()) {
                player.sendMessage(messages.get("setup-unknown-team", "team", teamId.get(), "teams", teams(session.draft())));
                return;
            }
            team = color.get();
        } else if (teamId.isPresent()) {
            player.sendMessage(messages.get("setup-no-team-needed"));
            return;
        }
        action.run(session, step.get(), team);
    }

    private Optional<SetupSession> editingSession(Player player) {
        Optional<SetupSession> session = sessionOf(player);
        if (session.isEmpty()) {
            player.sendMessage(messages.get("setup-not-editing"));
        }
        return session;
    }

    private String teams(ArenaDraft draft) {
        return String.join(", ", draft.teams().stream().map(TeamColor::id).toList());
    }

    private static String allColors() {
        List<String> ids = new ArrayList<>();
        for (TeamColor color : TeamColor.values()) {
            ids.add(color.id());
        }
        return String.join(", ", ids);
    }

    private void main(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    static String size(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @FunctionalInterface
    private interface StepAction {
        void run(SetupSession session, SetupStep step, TeamColor team);
    }
}
