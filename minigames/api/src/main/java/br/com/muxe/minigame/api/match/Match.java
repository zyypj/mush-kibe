package br.com.muxe.minigame.api.match;

import br.com.muxe.minigame.api.combat.KillCredit;
import br.com.muxe.minigame.api.match.Participant.State;
import br.com.muxe.minigame.engine.match.StartCountdown.Change;
import br.com.muxe.minigame.engine.match.StartCountdown;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.team.TeamAssigner;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.cell.Cell;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class Match {

    public static final int MANUAL_PLAYERS = 2;

    private final String code;
    private GameMode mode;
    private final MatchArena arena;
    private final Cell cell;
    private StartCountdown countdown;
    private final boolean respawns;
    private final KillCredit credit = new KillCredit();
    private final Map<UUID, Participant> players = new LinkedHashMap<>();
    private final Map<TeamColor, MatchTeam> teams = new LinkedHashMap<>();
    private final Set<UUID> unranked = new HashSet<>();
    private final Set<Long> placed = new HashSet<>();
    private final Map<TeamColor, String> teamNames = new HashMap<>();
    private MatchPhase phase = MatchPhase.WAITING;
    private String label;
    private boolean manual;
    private boolean forced;
    private boolean held;
    private int startingTeams;
    private int elapsed;
    private int idleSeconds;
    private UUID matchId;
    private int ending;
    private Instant startedAt;

    public Match(String code, GameMode mode, MatchArena arena, Cell cell, boolean respawns) {
        this(code, mode, arena, cell, respawns, arena.teamColors());
    }

    public Match(String code, GameMode mode, MatchArena arena, Cell cell, boolean respawns, List<TeamColor> colors) {
        this.code = code;
        this.label = code;
        this.mode = mode;
        this.arena = arena;
        this.cell = cell;
        this.countdown = new StartCountdown(mode);
        this.respawns = respawns;
        colors.forEach(color -> teams.put(color, new MatchTeam(color, respawns)));
    }

    public boolean reshape(GameMode changed, List<TeamColor> colors) {
        if (!phase.beforeStart() || players.size() > changed.maxPlayers()) {
            return false;
        }
        mode = changed;
        countdown = new StartCountdown(changed);
        phase = MatchPhase.WAITING;
        forced = false;
        teams.clear();
        colors.forEach(color -> teams.put(color, new MatchTeam(color, respawns)));
        updateCountdown();
        return true;
    }

    public String label() {
        return label;
    }

    public void label(String value) {
        label = value;
    }

    public boolean manual() {
        return manual;
    }

    public void manual(boolean value) {
        manual = value;
    }

    public String teamName(TeamColor color) {
        return teamNames.getOrDefault(color, color.displayName());
    }

    public char teamLetter(TeamColor color) {
        String name = teamName(color);
        return name.isEmpty() ? color.letter() : name.charAt(0);
    }

    public void teamNames(Map<TeamColor, String> names) {
        teamNames.clear();
        teamNames.putAll(names);
    }

    public boolean pause() {
        if (phase != MatchPhase.STARTING) {
            return false;
        }
        countdown.cancel();
        phase = MatchPhase.WAITING;
        forced = false;
        held = true;
        return true;
    }

    public String code() {
        return code;
    }

    public GameMode mode() {
        return mode;
    }

    public MatchArena arena() {
        return arena;
    }

    public Cell cell() {
        return cell;
    }

    public MatchPhase phase() {
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

    public Optional<UUID> matchId() {
        return Optional.ofNullable(matchId);
    }

    public boolean acceptsPlayers() {
        return phase.beforeStart() && players.size() < mode.maxPlayers();
    }

    public Collection<Participant> players() {
        return Collections.unmodifiableCollection(players.values());
    }

    public List<UUID> joinOrder() {
        return List.copyOf(players.keySet());
    }

    public Optional<Participant> player(UUID id) {
        return Optional.ofNullable(players.get(id));
    }

    public boolean isAlive(UUID id) {
        return player(id).map(Participant::isAlive).orElse(false);
    }

    public Map<TeamColor, MatchTeam> teams() {
        return Collections.unmodifiableMap(teams);
    }

    public MatchTeam team(TeamColor color) {
        return teams.get(color);
    }

    public Change join(UUID id, String name) {
        if (!acceptsPlayers() || players.containsKey(id)) {
            throw new IllegalStateException("Match " + code + " does not accept " + name);
        }
        players.put(id, new Participant(id, name));
        return updateCountdown();
    }

    public boolean idle(boolean reserved, int limitSeconds) {
        if (phase != MatchPhase.WAITING || !players.isEmpty() || reserved) {
            idleSeconds = 0;
            return false;
        }
        idleSeconds++;
        return idleSeconds >= limitSeconds;
    }

    public Change leaveBeforeStart(UUID id) {
        if (!phase.beforeStart()) {
            throw new IllegalStateException("Match " + code + " already started");
        }
        players.remove(id);
        return updateCountdown();
    }

    public Optional<Change> forceStart(int seconds) {
        if (!phase.beforeStart() || players.isEmpty()) {
            return Optional.empty();
        }
        OptionalInt before = countdown.remaining();
        countdown.force(seconds);
        phase = MatchPhase.STARTING;
        forced = true;
        if (before.isEmpty()) {
            return Optional.of(Change.STARTED);
        }
        return Optional.of(before.getAsInt() > seconds ? Change.SHORTENED : Change.NONE);
    }

    public void forget(UUID id) {
        players.remove(id);
    }

    public void start(List<List<UUID>> parties, Instant now) {
        start(parties, Map.of(), ThreadLocalRandom.current(), now);
    }

    public void start(List<List<UUID>> parties, Map<UUID, TeamColor> preferences, RandomGenerator random, Instant now) {
        if (!phase.beforeStart()) {
            throw new IllegalStateException("Match " + code + " already started");
        }
        matchId = UUID.randomUUID();
        Map<TeamColor, List<UUID>> assigned = TeamAssigner.assign(List.copyOf(teams.keySet()), mode.teamSize(), joinOrder(), parties, preferences, random);
        assigned.forEach((color, members) -> members.forEach(member -> {
            teams.get(color).add(member);
            Participant player = players.get(member);
            player.team(color);
            player.state(State.ALIVE);
        }));
        for (MatchTeam team : teams.values()) {
            if (team.members().isEmpty()) {
                team.stopRespawning(null);
            }
        }
        startingTeams = (int) teams.values().stream().filter(team -> !team.members().isEmpty()).count();
        phase = MatchPhase.PLAYING;
        startedAt = now;
        elapsed = 0;
    }

    public Set<UUID> splitParties(List<List<UUID>> parties) {
        Set<UUID> split = new HashSet<>();
        for (List<UUID> party : parties) {
            List<UUID> present = party.stream().filter(players::containsKey).toList();
            long teamsUsed = present.stream().map(players::get).map(Participant::team).distinct().count();
            if (present.size() > 1 && teamsUsed > 1) {
                split.addAll(present);
            }
        }
        return split;
    }

    public void unrank(Collection<UUID> ids) {
        unranked.addAll(ids);
    }

    public boolean ranked(UUID id) {
        return !unranked.contains(id);
    }

    public Set<UUID> unranked() {
        return Set.copyOf(unranked);
    }

    public void tick() {
        if (phase == MatchPhase.PLAYING) {
            elapsed++;
        } else if (phase == MatchPhase.ENDING) {
            ending++;
        }
    }

    public int endingFor() {
        return ending;
    }

    public void end() {
        phase = MatchPhase.ENDING;
        ending = 0;
    }

    public boolean respawns(Participant player) {
        return player.team() != null && teams.get(player.team()).respawns();
    }

    public void die(Participant player, int respawnSeconds) {
        boolean respawns = respawns(player);
        player.addDeath(!respawns);
        if (respawns) {
            player.state(State.RESPAWNING);
            player.respawnAt(elapsed + respawnSeconds);
        } else {
            player.state(State.ELIMINATED);
        }
        credit.forget(player.id());
    }

    public void respawned(Participant player, Instant protectedUntil) {
        player.state(State.ALIVE);
        player.protectUntil(protectedUntil);
    }

    public void protectAll(Instant until) {
        players.values().stream().filter(Participant::isAlive).forEach(player -> player.protectUntil(until));
    }

    public void struck(UUID attacker) {
        player(attacker).ifPresent(player -> player.protectUntil(Instant.MIN));
    }

    public void disconnected(Participant player, Instant until) {
        player.state(State.DISCONNECTED);
        player.disconnectedUntil(until);
    }

    public void reconnected(Participant player, int respawnSeconds) {
        player.state(State.RESPAWNING);
        player.respawnAt(elapsed + respawnSeconds);
    }

    public void eliminate(Participant player) {
        player.state(State.ELIMINATED);
        player.addDeath(true);
    }

    public boolean canReturn(UUID id, Instant now) {
        return phase == MatchPhase.PLAYING && player(id)
            .filter(player -> player.state() == State.DISCONNECTED)
            .filter(player -> player.disconnectedUntil().isAfter(now))
            .filter(this::respawns)
            .isPresent();
    }

    public boolean leavingWhileReturnable(UUID id) {
        return phase == MatchPhase.PLAYING && player(id).filter(Participant::isPlaying).filter(this::respawns).isPresent();
    }

    public Optional<Participant> killer(Participant victim, Instant now, boolean finalKill) {
        Optional<UUID> fallback = victim.team() == null ? Optional.empty() : teams.get(victim.team()).respawnTakenBy();
        return credit.killer(victim.id(), now, finalKill, fallback)
            .flatMap(this::player)
            .filter(killer -> killer.team() != victim.team());
    }

    public void assists(Participant victim, Participant killer, boolean finalKill, Instant now) {
        for (UUID helper : credit.assisters(victim.id(), killer.id(), now)) {
            Participant assister = players.get(helper);
            if (assister != null && assister.team() != victim.team()) {
                assister.addAssist(finalKill);
            }
        }
    }

    public void credit(Participant killer, boolean finalKill) {
        killer.addKill(finalKill);
    }

    public TeamStatus status(TeamColor color) {
        MatchTeam team = teams.get(color);
        int alive = 0;
        int connected = 0;
        boolean canReturn = false;
        for (UUID member : team.members()) {
            Participant player = players.get(member);
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
        return TeamStatus.of(team.respawns(), alive, connected, canReturn);
    }

    public List<TeamColor> standing() {
        return teams.keySet().stream().filter(color -> !status(color).isEliminated()).toList();
    }

    public boolean canForceEnd() {
        return phase == MatchPhase.PLAYING;
    }

    public boolean isOver() {
        int left = standing().size();
        return forced && startingTeams == 1 ? left == 0 : left <= 1;
    }

    public Optional<TeamColor> winner() {
        List<TeamColor> standing = standing();
        return standing.size() == 1 ? Optional.of(standing.getFirst()) : Optional.empty();
    }

    public Optional<UUID> celebrant(TeamColor color, Set<UUID> online, RandomGenerator random) {
        List<UUID> winners = members(color).stream().map(Participant::id).filter(online::contains).toList();
        return winners.isEmpty() ? Optional.empty() : Optional.of(winners.get(random.nextInt(winners.size())));
    }

    public List<Participant> members(TeamColor color) {
        List<Participant> members = new ArrayList<>();
        for (UUID member : teams.get(color).members()) {
            Participant player = players.get(member);
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
        if (forced) {
            return Change.NONE;
        }
        if (manual) {
            return manualCountdown();
        }
        Change change = countdown.players(players.size());
        if (change == Change.STARTED) {
            phase = MatchPhase.STARTING;
        } else if (change == Change.CANCELLED) {
            phase = MatchPhase.WAITING;
        }
        return change;
    }

    private Change manualCountdown() {
        boolean enough = players.size() >= MANUAL_PLAYERS;
        if (held || enough == countdown.isRunning()) {
            return Change.NONE;
        }
        if (enough) {
            countdown.force(StartCountdown.AT_MINIMUM);
            phase = MatchPhase.STARTING;
            return Change.STARTED;
        }
        countdown.cancel();
        phase = MatchPhase.WAITING;
        return Change.CANCELLED;
    }

    static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }
}
