package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.bedwars.arena.BedWarsArena;
import br.com.muxe.minigame.bedwars.game.KillCredit;
import br.com.muxe.minigame.bedwars.game.TeamStatus;
import br.com.muxe.minigame.bedwars.room.RoomPlayer.State;
import br.com.muxe.minigame.engine.match.StartCountdown;
import br.com.muxe.minigame.engine.match.StartCountdown.Change;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.team.TeamAssigner;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.cell.Cell;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class Room {

    public enum Phase {
        WAITING,
        STARTING,
        PLAYING,
        ENDING
    }

    private final String code;
    private final GameMode mode;
    private final BedWarsArena arena;
    private final Cell cell;
    private final StartCountdown countdown;
    private final KillCredit credit = new KillCredit();
    private final Map<UUID, RoomPlayer> players = new LinkedHashMap<>();
    private final Map<TeamColor, RoomTeam> teams = new LinkedHashMap<>();
    private final Set<Long> placed = new HashSet<>();
    private Phase phase = Phase.WAITING;
    private int elapsed;
    private int ending;
    private Instant startedAt;

    public Room(String code, GameMode mode, BedWarsArena arena, Cell cell) {
        this.code = code;
        this.mode = mode;
        this.arena = arena;
        this.cell = cell;
        this.countdown = new StartCountdown(mode);
        arena.teams().keySet().forEach(color -> teams.put(color, new RoomTeam(color)));
    }

    public String code() {
        return code;
    }

    public GameMode mode() {
        return mode;
    }

    public BedWarsArena arena() {
        return arena;
    }

    public Cell cell() {
        return cell;
    }

    public Phase phase() {
        return phase;
    }

    public StartCountdown countdown() {
        return countdown;
    }

    public KillCredit credit() {
        return credit;
    }

    public int elapsed() {
        return elapsed;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public boolean acceptsPlayers() {
        return (phase == Phase.WAITING || phase == Phase.STARTING) && players.size() < mode.maxPlayers();
    }

    public Collection<RoomPlayer> players() {
        return Collections.unmodifiableCollection(players.values());
    }

    public List<UUID> joinOrder() {
        return List.copyOf(players.keySet());
    }

    public Optional<RoomPlayer> player(UUID id) {
        return Optional.ofNullable(players.get(id));
    }

    public Map<TeamColor, RoomTeam> teams() {
        return Collections.unmodifiableMap(teams);
    }

    public RoomTeam team(TeamColor color) {
        return teams.get(color);
    }

    public Change join(UUID id, String name) {
        if (!acceptsPlayers() || players.containsKey(id)) {
            throw new IllegalStateException("Room " + code + " does not accept " + name);
        }
        players.put(id, new RoomPlayer(id, name));
        return updateCountdown();
    }

    public Change leaveBeforeStart(UUID id) {
        if (phase != Phase.WAITING && phase != Phase.STARTING) {
            throw new IllegalStateException("Room " + code + " already started");
        }
        players.remove(id);
        return updateCountdown();
    }

    public void forget(UUID id) {
        players.remove(id);
    }

    public void start(List<List<UUID>> parties, Instant now) {
        if (phase == Phase.PLAYING || phase == Phase.ENDING) {
            throw new IllegalStateException("Room " + code + " already started");
        }
        Map<TeamColor, List<UUID>> assigned = TeamAssigner.assign(List.copyOf(teams.keySet()), mode.teamSize(), joinOrder(), parties);
        assigned.forEach((color, members) -> members.forEach(member -> {
            teams.get(color).add(member);
            RoomPlayer player = players.get(member);
            player.team(color);
            player.state(State.ALIVE);
        }));
        for (RoomTeam team : teams.values()) {
            if (team.members().isEmpty()) {
                team.breakBed(null);
            }
        }
        phase = Phase.PLAYING;
        startedAt = now;
        elapsed = 0;
    }

    public void tick() {
        if (phase == Phase.PLAYING) {
            elapsed++;
        } else if (phase == Phase.ENDING) {
            ending++;
        }
    }

    public int endingFor() {
        return ending;
    }

    public void end() {
        phase = Phase.ENDING;
        ending = 0;
    }

    public void die(RoomPlayer player, int respawnSeconds) {
        RoomTeam team = teams.get(player.team());
        if (team.bedAlive()) {
            player.state(State.RESPAWNING);
            player.respawnAt(elapsed + respawnSeconds);
        } else {
            player.state(State.ELIMINATED);
        }
        player.downgradeTools();
        credit.forget(player.id());
    }

    public void respawned(RoomPlayer player, Instant protectedUntil) {
        player.state(State.ALIVE);
        player.protectUntil(protectedUntil);
    }

    public void disconnected(RoomPlayer player, Instant until) {
        player.state(State.DISCONNECTED);
        player.disconnectedUntil(until);
    }

    public void reconnected(RoomPlayer player, int respawnSeconds) {
        player.state(State.RESPAWNING);
        player.respawnAt(elapsed + respawnSeconds);
    }

    public void eliminate(RoomPlayer player) {
        player.state(State.ELIMINATED);
    }

    public void credit(RoomPlayer killer, boolean finalKill) {
        killer.addKill(finalKill);
    }

    public List<RoomPlayer> breakBed(TeamColor color, RoomPlayer breaker) {
        RoomTeam team = teams.get(color);
        team.breakBed(breaker == null ? null : breaker.id());
        if (breaker != null) {
            breaker.addBed();
        }
        List<RoomPlayer> lost = new ArrayList<>();
        for (UUID member : team.members()) {
            RoomPlayer player = players.get(member);
            if (player != null && player.state() == State.DISCONNECTED) {
                player.state(State.ELIMINATED);
                lost.add(player);
            }
        }
        return lost;
    }

    public TeamStatus status(TeamColor color) {
        RoomTeam team = teams.get(color);
        int alive = 0;
        int connected = 0;
        boolean canReturn = false;
        for (UUID member : team.members()) {
            RoomPlayer player = players.get(member);
            if (player == null || !player.isPlaying()) {
                continue;
            }
            alive++;
            if (player.state() == State.DISCONNECTED) {
                canReturn = true;
            } else {
                connected++;
            }
        }
        return TeamStatus.of(team.bedAlive(), alive, connected, canReturn);
    }

    public List<TeamColor> standing() {
        return teams.keySet().stream().filter(color -> !status(color).isEliminated()).toList();
    }

    public Optional<TeamColor> winner() {
        List<TeamColor> standing = standing();
        return standing.size() == 1 ? Optional.of(standing.getFirst()) : Optional.empty();
    }

    public List<RoomPlayer> members(TeamColor color) {
        List<RoomPlayer> members = new ArrayList<>();
        for (UUID member : teams.get(color).members()) {
            RoomPlayer player = players.get(member);
            if (player != null) {
                members.add(player);
            }
        }
        return members;
    }

    public void placed(int x, int y, int z) {
        placed.add(key(x, y, z));
    }

    public boolean wasPlaced(int x, int y, int z) {
        return placed.contains(key(x, y, z));
    }

    public void unplaced(int x, int y, int z) {
        placed.remove(key(x, y, z));
    }

    private Change updateCountdown() {
        Change change = countdown.players(players.size());
        if (change == Change.STARTED) {
            phase = Phase.STARTING;
        } else if (change == Change.CANCELLED) {
            phase = Phase.WAITING;
        }
        return change;
    }

    static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }
}
