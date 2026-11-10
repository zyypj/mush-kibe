package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.minigame.api.match.ChatView;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchArena;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.matchmaking.Reservations;
import br.com.muxe.minigame.api.matchmaking.RoomChoice;
import br.com.muxe.minigame.api.network.ArenaInfo;
import br.com.muxe.minigame.api.network.Reserved;
import br.com.muxe.minigame.api.network.RoomInfo;
import br.com.muxe.minigame.api.network.RoomSource;
import br.com.muxe.minigame.api.view.NameTagStyle;
import br.com.muxe.minigame.engine.arena.ArenaCatalog;
import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.match.StartCountdown;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.room.RoomCodes;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.cell.Cell;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Location;

public final class MatchService<A extends MatchArena, S extends MatchState> implements Matches<S>, RoomSource {

    public static final Duration RESERVATION = Duration.ofSeconds(30);

    private final Minigame<A, S> minigame;
    private final Map<String, GameMode> modes;
    private final ArenaCatalog catalog;
    private final Cells cells;
    private final Function<Match, Stage> stages;
    private final MatchEnvironment environment;
    private final BiConsumer<UUID, String> tell;
    private final Logger logger;
    private final RoomCodes codes;
    private final Map<String, MatchRuntime<S>> rooms = new LinkedHashMap<>();
    private final Map<UUID, String> byPlayer = new ConcurrentHashMap<>();
    private final Reservations reservations = new Reservations(RESERVATION);
    private final Map<UUID, Watch> watching = new ConcurrentHashMap<>();
    private final Set<UUID> shouting = ConcurrentHashMap.newKeySet();
    private final Set<String> hidden = ConcurrentHashMap.newKeySet();
    private volatile Map<UUID, ChatView> chat = Map.of();
    private volatile int activeMatches;

    public MatchService(Minigame<A, S> minigame, List<GameMode> modes, ArenaCatalog catalog, Cells cells, Function<Match, Stage> stages,
                        MatchEnvironment environment, BiConsumer<UUID, String> tell, Logger logger) {
        this.minigame = minigame;
        this.modes = modes.stream().collect(Collectors.toMap(GameMode::id, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        this.catalog = catalog;
        this.cells = cells;
        this.stages = stages;
        this.environment = environment;
        this.tell = tell;
        this.logger = logger;
        this.codes = new RoomCodes(environment.random());
    }

    public Minigame<A, S> minigame() {
        return minigame;
    }

    public MatchEnvironment environment() {
        return environment;
    }

    public List<String> modes() {
        return List.copyOf(modes.keySet());
    }

    public Optional<GameMode> mode(String id) {
        return Optional.ofNullable(modes.get(id));
    }

    public String modeName(String id) {
        return mode(id).map(GameMode::displayName).orElse(id);
    }

    public void arrive(UUID id, String name) {
        Instant now = environment.clock().instant();
        Optional<Reservations.Reservation> reserved = reservations.take(id, now);
        if (reserved.isPresent()) {
            join(id, name, reserved.get().mode(), reserved.get().arena(), Optional.of(reserved.get().code()));
            return;
        }
        Watch watch = watching.remove(id);
        if (watch != null && now.isBefore(watch.expiresAt()) && rooms.containsKey(watch.code())) {
            watch(id, rooms.get(watch.code()));
            return;
        }
        for (MatchRuntime<S> runtime : rooms.values()) {
            if (runtime.canReturn(id, now)) {
                byPlayer.put(id, runtime.match().code());
                runtime.rejoin(id);
                refreshChat();
                return;
            }
        }
        join(id, name, environment.settings().defaultMode());
    }

    public Optional<Reserved> reserve(UUID playerId, String mode, Optional<String> arena, Optional<String> code) {
        return reserve(playerId, List.of(), mode, arena, code);
    }

    @Override
    public Optional<Reserved> reserve(UUID playerId, List<UUID> companions, String mode, Optional<String> arena, Optional<String> code) {
        GameMode gameMode = modes.get(mode);
        int seats = 1 + companions.size();
        if (gameMode == null || gameMode.maxPlayers() < seats) {
            return Optional.empty();
        }
        Instant now = environment.clock().instant();
        reservations.take(playerId, now);
        companions.forEach(companion -> reservations.take(companion, now));
        Optional<Reserved> reserved = pick(mode, arena, code, seats).map(found -> new Reserved(found.match().code(), false))
            .or(() -> create(mode, arena).map(created -> new Reserved(created.match().code(), true)));
        reserved.ifPresent(found -> {
            reservations.hold(playerId, found.code(), mode, arena, now);
            companions.forEach(companion -> reservations.hold(companion, found.code(), mode, arena, now));
        });
        return reserved;
    }

    public List<UUID> gather(UUID leader, List<UUID> members) {
        MatchRuntime<S> runtime = of(leader).orElse(null);
        if (runtime == null) {
            return members;
        }
        Instant now = environment.clock().instant();
        Match match = runtime.match();
        List<UUID> accepted = new ArrayList<>();
        for (UUID member : members) {
            if (runtime.isWatching(member) || match.player(member).isPresent() && runtime.online().contains(member)) {
                continue;
            }
            if (runtime.canReturn(member, now)) {
                reservations.take(member, now);
                watching.remove(member);
                accepted.add(member);
                continue;
            }
            if (match.phase().beforeStart()) {
                int taken = match.players().size() + reservations.pending(match.code(), now);
                if (!match.acceptsPlayers() || taken >= match.mode().maxPlayers()) {
                    continue;
                }
                reservations.hold(member, match.code(), match.mode().id(), Optional.of(match.arena().name()), now);
            } else {
                reservations.take(member, now);
                watching.put(member, new Watch(match.code(), now.plus(RESERVATION)));
            }
            accepted.add(member);
        }
        return accepted;
    }

    public void watch(UUID id, MatchRuntime<S> runtime) {
        byPlayer.put(id, runtime.match().code());
        runtime.watch(id);
        refreshChat();
    }

    @Override
    public List<RoomInfo> summaries() {
        Instant now = environment.clock().instant();
        return rooms.values().stream().map(runtime -> new RoomInfo(
            runtime.match().code(),
            runtime.match().mode().id(),
            runtime.match().arena().name(),
            runtime.match().acceptsPlayers() && !hidden.contains(runtime.match().code()),
            runtime.match().players().size() + reservations.pending(runtime.match().code(), now),
            runtime.match().mode().maxPlayers()
        )).toList();
    }

    @Override
    public List<ArenaInfo> arenas() {
        return catalog.all().stream().map(loaded -> new ArenaInfo(loaded.mode(), loaded.name(), loaded.displayName())).toList();
    }

    public void join(UUID id, String name, String mode) {
        join(id, name, mode, Optional.empty(), Optional.empty());
    }

    public boolean forceStart(UUID id) {
        return of(id).map(runtime -> runtime.forceStart(StartCountdown.WHEN_FULL)).orElse(false);
    }

    public boolean forceEnd(UUID id) {
        return of(id).map(MatchRuntime::forceEnd).orElse(false);
    }

    public void switchTo(UUID id, String name, String mode) {
        of(id).ifPresent(runtime -> {
            if (runtime.match().phase().beforeStart()) {
                leave(id);
            }
        });
        if (of(id).isEmpty()) {
            join(id, name, mode);
        }
    }

    public int activeMatches() {
        return activeMatches;
    }

    public void shout(UUID id, Runnable chat) {
        shouting.add(id);
        try {
            chat.run();
        } finally {
            shouting.remove(id);
        }
    }

    public boolean isShouting(UUID id) {
        return shouting.contains(id);
    }

    public Optional<ChatView> chatView(UUID id) {
        return Optional.ofNullable(chat.get(id));
    }

    public String chatBadge(UUID id) {
        return of(id).map(runtime -> runtime.state().chatBadge(id)).orElse("");
    }

    public void leave(UUID id) {
        String code = byPlayer.get(id);
        MatchRuntime<S> runtime = code == null ? null : rooms.get(code);
        if (runtime == null) {
            byPlayer.remove(id);
            return;
        }
        runtime.quit(id);
        boolean keep = runtime.match().player(id).map(Participant::state).filter(state -> state == Participant.State.DISCONNECTED).isPresent();
        if (!keep) {
            byPlayer.remove(id);
        }
        refreshChat();
        runtime.stage().refreshNameTag(id);
    }

    public Optional<NameTag> nameTag(UUID id) {
        return of(id)
            .filter(runtime -> runtime.match().phase().showsTeams())
            .flatMap(runtime -> runtime.match().player(id)
                .map(Participant::team)
                .map(team -> NameTagStyle.of(minigame.rules().nameTagPrefix(), team, runtime.match().teamLetter(team))));
    }

    @Override
    public Optional<MatchRuntime<S>> of(UUID player) {
        String code = byPlayer.get(player);
        return code == null ? Optional.empty() : Optional.ofNullable(rooms.get(code));
    }

    @Override
    public Optional<MatchRuntime<S>> at(Location location) {
        Optional<Cell> cell = cells.cellAt(location);
        if (cell.isEmpty()) {
            return Optional.empty();
        }
        for (MatchRuntime<S> runtime : rooms.values()) {
            if (runtime.match().cell().index() == cell.get().index()) {
                return Optional.of(runtime);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isMatchWorld(Location location) {
        return cells.contains(location);
    }

    @Override
    public List<MatchRuntime<S>> all() {
        return List.copyOf(rooms.values());
    }

    public void second() {
        Instant now = environment.clock().instant();
        reservations.purge(now);
        watching.values().removeIf(watch -> !now.isBefore(watch.expiresAt()));
        for (MatchRuntime<S> runtime : List.copyOf(rooms.values())) {
            try {
                if (runtime.match().idle(reservations.pending(runtime.match().code(), now) > 0, environment.settings().timings().emptyRoomSeconds())) {
                    runtime.close();
                    continue;
                }
                runtime.second();
            } catch (RuntimeException exception) {
                logger.log(Level.SEVERE, "Match " + runtime.match().code() + " failed and is being closed", exception);
                runtime.close();
            }
        }
        refreshChat();
        activeMatches = (int) rooms.values().stream().filter(runtime -> runtime.match().phase().showsTeams()).count();
    }

    public void fastTick() {
        for (MatchRuntime<S> runtime : List.copyOf(rooms.values())) {
            runtime.fastTick();
        }
    }

    public void stop() {
        for (MatchRuntime<S> runtime : List.copyOf(rooms.values())) {
            runtime.close();
        }
    }

    public Optional<MatchRuntime<S>> room(String code) {
        return Optional.ofNullable(rooms.get(code));
    }

    public Optional<MatchRuntime<S>> openPrivate(Optional<String> code, String mode, List<String> arenas, RoomShape shape, Consumer<MatchRuntime<S>> prepare) {
        GameMode base = modes.get(mode);
        if (base == null) {
            return Optional.empty();
        }
        List<LoadedArena> candidates = catalog.ofMode(mode).stream().filter(loaded -> arenas.isEmpty() || arenas.contains(loaded.name())).toList();
        if (candidates.isEmpty()) {
            candidates = catalog.ofMode(mode);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        LoadedArena loaded = candidates.get(environment.random().nextInt(candidates.size()));
        A arena;
        try {
            arena = minigame.arena(loaded);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Arena " + loaded.name() + " cannot be played", exception);
            return Optional.empty();
        }
        String chosen = code.filter(wanted -> !rooms.containsKey(wanted)).orElseGet(() -> codes.next(rooms.keySet()));
        Cell cell = cells.allocate(loaded.map());
        Match match = new Match(chosen, shape.mode(arena.teamColors()), arena, cell, minigame.rules().respawn(), shape.teams(arena.teamColors()));
        hidden.add(chosen);
        MatchRuntime<S> runtime = new MatchRuntime<>(match, stages.apply(match), environment, this::closed, opened -> minigame.open(opened, arena));
        prepare.accept(runtime);
        rooms.put(chosen, runtime);
        return Optional.of(runtime);
    }

    public void hold(UUID player, MatchRuntime<S> runtime) {
        Instant now = environment.clock().instant();
        reservations.take(player, now);
        reservations.hold(player, runtime.match().code(), runtime.match().mode().id(), Optional.of(runtime.match().arena().name()), now);
    }

    public void place(UUID id, String name, MatchRuntime<S> runtime) {
        leave(id);
        byPlayer.put(id, runtime.match().code());
        runtime.join(id, name);
        refreshChat();
    }

    private void join(UUID id, String name, String mode, Optional<String> arena, Optional<String> code) {
        MatchRuntime<S> runtime = code.filter(hidden::contains).map(rooms::get).filter(found -> found.match().acceptsPlayers())
            .or(() -> pick(mode, arena, code)).or(() -> create(mode, arena)).or(() -> pick(mode, Optional.empty(), Optional.empty())).orElse(null);
        if (runtime == null) {
            tell.accept(id, environment.messages().get("no-arena", "mode", modeName(mode)));
            return;
        }
        byPlayer.put(id, runtime.match().code());
        runtime.join(id, name);
        refreshChat();
    }

    private void refreshChat() {
        Map<UUID, ChatView> views = new HashMap<>();
        for (MatchRuntime<S> runtime : rooms.values()) {
            Set<UUID> online = Set.copyOf(runtime.online());
            views.putAll(ChatView.of(runtime.match(), online));
            runtime.watchers().forEach(watcher -> views.put(watcher, new ChatView(online, online, "")));
        }
        chat = Map.copyOf(views);
    }

    private void closed(MatchRuntime<S> runtime) {
        if (rooms.get(runtime.match().code()) != runtime) {
            return;
        }
        rooms.remove(runtime.match().code());
        hidden.remove(runtime.match().code());
        byPlayer.values().removeIf(code -> code.equals(runtime.match().code()));
        try {
            cells.release(runtime.match().cell());
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Could not release the cell of match " + runtime.match().code(), exception);
        }
    }

    private Optional<MatchRuntime<S>> pick(String mode, Optional<String> arena, Optional<String> code) {
        return pick(mode, arena, code, 1);
    }

    private Optional<MatchRuntime<S>> pick(String mode, Optional<String> arena, Optional<String> code, int seats) {
        return RoomChoice.pick(summaries(), mode, arena, code, seats).map(info -> rooms.get(info.code()));
    }

    private Optional<MatchRuntime<S>> create(String mode, Optional<String> wanted) {
        GameMode gameMode = modes.get(mode);
        if (gameMode == null) {
            return Optional.empty();
        }
        List<LoadedArena> arenas = catalog.ofMode(mode).stream().filter(loaded -> wanted.map(loaded.name()::equals).orElse(true)).toList();
        if (arenas.isEmpty()) {
            return Optional.empty();
        }
        LoadedArena loaded = arenas.get(environment.random().nextInt(arenas.size()));
        A arena;
        try {
            arena = minigame.arena(loaded);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Arena " + loaded.name() + " cannot be played", exception);
            return Optional.empty();
        }
        Cell cell = cells.allocate(loaded.map());
        String code = codes.next(rooms.keySet());
        Match match = new Match(code, gameMode, arena, cell, minigame.rules().respawn());
        MatchRuntime<S> runtime = new MatchRuntime<>(match, stages.apply(match), environment, this::closed, opened -> minigame.open(opened, arena));
        rooms.put(code, runtime);
        return Optional.of(runtime);
    }

    private record Watch(String code, Instant expiresAt) {
    }

    public interface RoomShape {

        GameMode mode(List<TeamColor> arenaTeams);

        List<TeamColor> teams(List<TeamColor> arenaTeams);
    }
}
