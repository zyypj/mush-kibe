package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.npc.Npc;
import br.com.muxe.core.bukkit.npc.NpcService;
import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.minigame.bedwars.arena.BedWarsArena;
import br.com.muxe.minigame.bedwars.arena.BedWarsSchema;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.engine.arena.ArenaCatalog;
import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.match.StartCountdown;
import br.com.muxe.minigame.engine.room.RoomCodes;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.cell.Cell;
import br.com.muxe.minigame.engine.world.server.MatchWorld;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RoomService {

    private final JavaPlugin plugin;
    private final MuxeCore core;
    private final BedWarsSettings settings;
    private final MessageCatalog messages;
    private final BedWarsSchema schema;
    private final ArenaCatalog catalog;
    private final MatchWorld matchWorld;
    private final RoomCodes codes = new RoomCodes(ThreadLocalRandom.current());
    private final Map<String, RoomRuntime> rooms = new LinkedHashMap<>();
    private final Map<UUID, String> byPlayer = new ConcurrentHashMap<>();
    private final Map<String, List<String>> npcLines = new ConcurrentHashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private Predicate<Player> busy = player -> false;
    private BiConsumer<Player, NpcClick> npcClicks = (player, click) -> {
    };
    private NpcService npcs;

    public RoomService(JavaPlugin plugin, MuxeCore core, BedWarsSettings settings, MessageCatalog messages, BedWarsSchema schema, ArenaCatalog catalog, MatchWorld matchWorld) {
        this.plugin = plugin;
        this.core = core;
        this.settings = settings;
        this.messages = messages;
        this.schema = schema;
        this.catalog = catalog;
        this.matchWorld = matchWorld;
    }

    public void start() {
        matchWorld.start();
        npcs = NpcService.start(plugin, npc -> npcLines.getOrDefault(npc.id(), List.of()));
        npcs.onClick(this::clicked);
        tasks.add(plugin.getServer().getScheduler().runTaskTimer(plugin, this::second, 20L, 20L));
        tasks.add(plugin.getServer().getScheduler().runTaskTimer(plugin, this::forgeCycle, 5L, 5L));
        core.nameTags().override(this::nameTag);
    }

    public void stop() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        for (RoomRuntime runtime : List.copyOf(rooms.values())) {
            runtime.close();
        }
        core.nameTags().clearOverride();
        if (npcs != null) {
            npcs.stop();
        }
        matchWorld.stop();
    }

    public void busyWhen(Predicate<Player> busy) {
        this.busy = busy;
    }

    public void onNpcClick(BiConsumer<Player, NpcClick> handler) {
        this.npcClicks = handler;
    }

    public void arrive(Player player) {
        if (busy.test(player)) {
            return;
        }
        Instant now = core.clock().instant();
        for (RoomRuntime runtime : rooms.values()) {
            if (runtime.canReturn(player.getUniqueId(), now)) {
                byPlayer.put(player.getUniqueId(), runtime.room().code());
                runtime.rejoin(player);
                return;
            }
        }
        join(player, settings.defaultMode());
    }

    public void join(Player player, String mode) {
        Optional<RoomRuntime> open = rooms.values().stream()
            .filter(runtime -> runtime.room().mode().id().equals(mode) && runtime.room().acceptsPlayers())
            .max(Comparator.comparingInt(runtime -> runtime.room().players().size()));
        RoomRuntime runtime = open.or(() -> create(mode)).orElse(null);
        if (runtime == null) {
            player.sendMessage(messages.get("no-arena", "mode", schema.modes().contains(mode) ? schema.mode(mode).displayName() : mode));
            return;
        }
        byPlayer.put(player.getUniqueId(), runtime.room().code());
        runtime.addWaiting(player);
    }

    public boolean forceStart(Player player) {
        return roomOf(player.getUniqueId()).map(runtime -> runtime.room().forceStart(StartCountdown.WHEN_FULL)).orElse(false);
    }

    public List<String> modes() {
        return schema.modes();
    }

    public void switchTo(Player player, String mode) {
        roomOf(player.getUniqueId()).ifPresent(runtime -> {
            if (runtime.room().phase() == Room.Phase.WAITING || runtime.room().phase() == Room.Phase.STARTING) {
                leave(player);
            }
        });
        if (roomOf(player.getUniqueId()).isEmpty()) {
            join(player, mode);
        }
    }

    public void leave(Player player) {
        String code = byPlayer.get(player.getUniqueId());
        RoomRuntime runtime = code == null ? null : rooms.get(code);
        if (runtime == null) {
            byPlayer.remove(player.getUniqueId());
            return;
        }
        runtime.quit(player);
        boolean keep = runtime.room().player(player.getUniqueId()).map(RoomPlayer::state).filter(state -> state == RoomPlayer.State.DISCONNECTED).isPresent();
        if (!keep) {
            byPlayer.remove(player.getUniqueId());
        }
        core.nameTags().refresh(player);
    }

    public Optional<RoomRuntime> roomOf(UUID player) {
        String code = byPlayer.get(player);
        return code == null ? Optional.empty() : Optional.ofNullable(rooms.get(code));
    }

    public Optional<RoomRuntime> roomAt(Location location) {
        Optional<Cell> cell = matchWorld.cellAt(location);
        if (cell.isEmpty()) {
            return Optional.empty();
        }
        for (RoomRuntime runtime : rooms.values()) {
            if (runtime.room().cell().index() == cell.get().index()) {
                return Optional.of(runtime);
            }
        }
        return Optional.empty();
    }

    public boolean isMatchWorld(Location location) {
        return matchWorld.contains(location.getWorld());
    }

    public void arenaSaved(String name) {
        catalog.reload(name);
    }

    MuxeCore core() {
        return core;
    }

    BedWarsSettings settings() {
        return settings;
    }

    MessageCatalog messages() {
        return messages;
    }

    JavaPlugin plugin() {
        return plugin;
    }

    NpcService npcs() {
        return npcs;
    }

    Map<String, List<String>> npcLines() {
        return npcLines;
    }

    void closed(RoomRuntime runtime) {
        rooms.remove(runtime.room().code());
        byPlayer.values().removeIf(code -> code.equals(runtime.room().code()));
        try {
            matchWorld.release(runtime.room().cell());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not release the cell of room " + runtime.room().code(), exception);
        }
    }

    <T> void batch(List<T> items, int perTick, Consumer<T> action) {
        Iterator<T> iterator = items.iterator();
        BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (int count = 0; count < perTick && iterator.hasNext(); count++) {
                action.accept(iterator.next());
            }
            if (!iterator.hasNext()) {
                task[0].cancel();
            }
        }, 0L, 1L);
    }

    private Optional<RoomRuntime> create(String mode) {
        if (!schema.modes().contains(mode)) {
            return Optional.empty();
        }
        List<LoadedArena> arenas = catalog.ofMode(mode);
        if (arenas.isEmpty()) {
            return Optional.empty();
        }
        LoadedArena loaded = arenas.get(ThreadLocalRandom.current().nextInt(arenas.size()));
        BedWarsArena arena;
        try {
            arena = BedWarsArena.from(loaded);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Arena " + loaded.name() + " cannot be played", exception);
            return Optional.empty();
        }
        Cell cell = matchWorld.allocate(loaded.map());
        String code = codes.next(rooms.keySet());
        Room room = new Room(code, schema.mode(mode), arena, cell);
        RoomRuntime runtime = new RoomRuntime(this, room, matchWorld.world());
        rooms.put(code, runtime);
        return Optional.of(runtime);
    }

    private void second() {
        for (RoomRuntime runtime : List.copyOf(rooms.values())) {
            try {
                runtime.second();
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Room " + runtime.room().code() + " failed and is being closed", exception);
                runtime.close();
            }
        }
    }

    private void forgeCycle() {
        for (RoomRuntime runtime : rooms.values()) {
            runtime.forgeCycle();
        }
    }

    private void clicked(Player player, Npc npc) {
        roomOf(player.getUniqueId()).ifPresent(runtime -> {
            if (runtime.npcRoom(npc.id()).isEmpty() || runtime.room().phase() != Room.Phase.PLAYING) {
                return;
            }
            boolean upgrades = npc.id().contains("-melhorias");
            npcClicks.accept(player, new NpcClick(runtime, upgrades));
        });
    }

    private Optional<NameTag> nameTag(UUID id) {
        return roomOf(id)
            .filter(runtime -> runtime.room().phase() == Room.Phase.PLAYING || runtime.room().phase() == Room.Phase.ENDING)
            .flatMap(runtime -> runtime.room().player(id))
            .map(RoomPlayer::team)
            .map(RoomService::tag);
    }

    static NameTag tag(TeamColor team) {
        String name = String.format("mxbw%02d%s", team.ordinal(), team.id());
        return new NameTag(name.length() > NameTag.MAX_TEAM ? name.substring(0, NameTag.MAX_TEAM) : name,
            Colors.translate(team.color() + team.letter() + " " + team.color()));
    }

    public record NpcClick(RoomRuntime runtime, boolean upgrades) {
    }
}
