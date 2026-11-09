package br.com.muxe.minigame.api.runtime;

import br.com.muxe.core.bukkit.cosmetic.animation.Area;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.MatchPhase;
import br.com.muxe.minigame.api.match.Participant.State;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.match.Ranking;
import br.com.muxe.minigame.api.runtime.Death.Cause;
import br.com.muxe.minigame.api.runtime.Stage.Look;
import br.com.muxe.minigame.api.settings.MatchSettings;
import br.com.muxe.minigame.api.view.MatchBoard;
import br.com.muxe.minigame.engine.match.StartCountdown.Change;
import br.com.muxe.minigame.engine.match.StartCountdown.Second;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class MatchRuntime<S extends MatchState> {

    public static final double AREA_MARGIN = 12;
    public static final int CLOSE_AFTER_RESTART = 3;
    public static final double DEATH_LIFT = 1.5;

    private final Match match;
    private final Stage stage;
    private final MatchEnvironment environment;
    private final MatchSettings settings;
    private final MessageCatalog messages;
    private final MatchBoard board;
    private final Consumer<MatchRuntime<S>> onClose;
    private final Set<TeamColor> announcedEliminations = new HashSet<>();
    private final Set<UUID> returnable = new HashSet<>();
    private final Set<UUID> boards = new HashSet<>();
    private final Set<UUID> watchers = new LinkedHashSet<>();
    private final S state;
    private Optional<KnockbackProfile> knockback;
    private RoomControl control = RoomControl.STANDARD;
    private int seconds;
    private boolean closed;

    public MatchRuntime(Match match, Stage stage, MatchEnvironment environment, Consumer<MatchRuntime<S>> onClose, Function<MatchRuntime<S>, S> opener) {
        this.match = match;
        this.stage = stage;
        this.environment = environment;
        this.settings = environment.settings();
        this.messages = environment.messages();
        this.board = new MatchBoard(settings.board());
        this.onClose = onClose;
        this.knockback = environment.knockback().forMode(match.mode().id());
        this.state = opener.apply(this);
    }

    public Match match() {
        return match;
    }

    public S state() {
        return state;
    }

    public Stage stage() {
        return stage;
    }

    public MatchEnvironment environment() {
        return environment;
    }

    public MessageCatalog messages() {
        return messages;
    }

    public RoomControl control() {
        return control;
    }

    public void control(RoomControl value) {
        control = value;
    }

    public void giveWaitingItems(UUID id) {
        if (match.phase().beforeStart() && match.player(id).isPresent()) {
            stage.waitingItems(id, control.waitingItems(id, settings.waitingHotbar().items()));
        }
    }

    public Optional<KnockbackProfile> knockback() {
        return knockback;
    }

    public void knockback(KnockbackProfile profile) {
        knockback = Optional.of(profile);
        for (UUID id : online()) {
            if (match.player(id).isPresent()) {
                stage.knockback(id, knockback);
            }
        }
    }

    public World world() {
        return stage.world();
    }

    public Location location(Point point) {
        return stage.location(point);
    }

    public Optional<Player> player(UUID id) {
        return stage.player(id);
    }

    public List<UUID> online() {
        if (watchers.isEmpty()) {
            return stage.present(match.joinOrder());
        }
        List<UUID> everyone = new ArrayList<>(match.joinOrder());
        everyone.addAll(watchers);
        return stage.present(everyone);
    }

    public Set<UUID> watchers() {
        return Set.copyOf(watchers);
    }

    public boolean isWatching(UUID id) {
        return watchers.contains(id);
    }

    public void watch(UUID id) {
        watchers.add(id);
        stage.prepare(id, Look.SPECTATOR);
        stage.teleport(id, match.arena().spectator());
        stage.spectatorItems(id, settings.spectatorHotbar().spectatorItems());
        stage.showBoard(id);
        boards.add(id);
        refreshVisibility();
        refreshBoards();
    }

    public List<Player> onlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID id : online()) {
            stage.player(id).ifPresent(players::add);
        }
        return players;
    }

    public void broadcast(String message) {
        online().forEach(id -> stage.message(id, message));
    }

    public String coloredName(Participant player) {
        return Colors.translate(player.team() == null ? "&7" : player.team().color()) + player.name();
    }

    public boolean isSpectator(UUID id) {
        if (watchers.contains(id)) {
            return true;
        }
        if (!match.phase().showsTeams()) {
            return false;
        }
        return !match.isAlive(id);
    }

    public boolean belowVoid(double y) {
        return y <= match.arena().voidY();
    }

    public void join(UUID id, String name) {
        Change change = match.join(id, name);
        stage.prepare(id, Look.PLAYER);
        stage.knockback(id, knockback);
        String joined = messages.get("room-joined", "player", stage.rankColored(id, name),
            "players", String.valueOf(match.players().size()), "max", String.valueOf(match.mode().maxPlayers()));
        Set<UUID> audience = new LinkedHashSet<>(online());
        audience.add(id);
        audience.forEach(viewer -> stage.message(viewer, joined));
        stage.teleport(id, match.arena().waitingSpawn());
        stage.waitingItems(id, control.waitingItems(id, settings.waitingHotbar().items()));
        stage.showBoard(id);
        boards.add(id);
        state.arrived(id);
        countdownChanged(change);
        refreshBoards();
    }

    public void quit(UUID id) {
        if (watchers.remove(id)) {
            forget(id);
            return;
        }
        Optional<Participant> found = match.player(id);
        forget(id);
        if (found.isEmpty()) {
            return;
        }
        Participant quitter = found.get();
        switch (match.phase()) {
            case WAITING, STARTING -> leaveBeforeStart(quitter);
            case ENDING -> {
            }
            case PLAYING -> {
                if (!quitter.isPlaying()) {
                    return;
                }
                Instant now = environment.clock().instant();
                if (match.respawns(quitter)) {
                    match.disconnected(quitter, now.plusSeconds(settings.timings().rejoinSeconds()));
                    rememberReturn(id);
                } else {
                    Optional<Participant> killer = match.killer(quitter, now, true);
                    match.eliminate(quitter);
                    killer.ifPresent(credited -> {
                        match.assists(quitter, credited, true, now);
                        match.credit(credited, true);
                    });
                    Death death = new Death(quitter, killer, true, Cause.QUIT);
                    state.died(death);
                    announce(death);
                    checkEliminations();
                }
                refreshBoards();
            }
        }
    }

    public boolean canReturn(UUID id, Instant now) {
        return match.canReturn(id, now);
    }

    public void rejoin(UUID id) {
        Participant player = match.player(id).orElseThrow();
        match.reconnected(player, settings.timings().respawnSeconds());
        forgetReturn(id);
        stage.knockback(id, knockback);
        ghost(id, false);
        stage.teleport(id, match.arena().spectator());
        stage.showBoard(id);
        boards.add(id);
        state.arrived(id);
        stage.refreshNameTag(id);
        broadcast(messages.get("reconnected", "player", coloredName(player)));
        refreshBoards();
    }

    public void second() {
        seconds++;
        state.everySecond();
        switch (match.phase()) {
            case WAITING -> refreshBoards();
            case STARTING -> {
                Second second = match.countdown().tick();
                if (second.finished()) {
                    start();
                } else {
                    settings.countdownTitles().at(second.remaining()).ifPresent(text -> countdownTitle(text, second.remaining()));
                    refreshBoards();
                }
            }
            case PLAYING -> {
                playingSecond();
                forgetLostReturns();
            }
            case ENDING -> {
                forgetLostReturns();
                match.tick();
                int restart = settings.timings().restartSeconds();
                if (match.endingFor() == restart && control.restartsAtEnd()) {
                    control.restart();
                } else if (match.endingFor() == restart) {
                    online().forEach(id -> stage.sendTo(id, settings.lobbyGroup()));
                } else if (match.endingFor() >= restart + CLOSE_AFTER_RESTART) {
                    close();
                }
            }
        }
    }

    public int seconds() {
        return seconds;
    }

    public void fastTick() {
        if (match.phase() != MatchPhase.PLAYING) {
            return;
        }
        state.fastTick();
        List<UUID> online = online();
        List<UUID> alive = online.stream().filter(match::isAlive).toList();
        List<UUID> viewers = online.stream().filter(boards::contains).toList();
        stage.health(viewers, alive);
    }

    public void die(UUID id, boolean voidDeath) {
        Optional<Participant> found = match.player(id).filter(Participant::isAlive);
        if (found.isEmpty()) {
            return;
        }
        Participant dead = found.get();
        boolean finalKill = !match.respawns(dead);
        Instant now = environment.clock().instant();
        Optional<Participant> killer = match.killer(dead, now, finalKill);
        Death death = new Death(dead, killer, finalKill, voidDeath ? Cause.VOID : Cause.DAMAGE);
        announce(death);
        if (finalKill && !voidDeath && killer.isPresent()) {
            Optional<Player> killerPlayer = stage.player(killer.get().id());
            Optional<Player> victimPlayer = stage.player(id);
            if (killerPlayer.isPresent() && victimPlayer.isPresent()) {
                environment.events().finalKill(killerPlayer.get(), victimPlayer.get(), dead.team(), onlinePlayers());
            }
        }
        killer.ifPresent(credited -> {
            match.assists(dead, credited, finalKill, now);
            match.credit(credited, finalKill);
        });
        state.died(death);
        match.die(dead, settings.timings().respawnSeconds());
        ghost(id, dead.state() != State.RESPAWNING);
        if (voidDeath) {
            stage.raiseTo(id, floor(dead));
        } else {
            stage.knockUp(id, DEATH_LIFT);
        }
        if (dead.state() == State.RESPAWNING) {
            stage.sound(id, settings.sounds().died());
            respawnCountdown(dead, settings.timings().respawnSeconds());
        } else {
            stage.title(id, Title.of(messages.get("died-title"), messages.get("spectator-subtitle")));
            stage.sound(id, settings.sounds().eliminated());
            checkEliminations();
        }
        refreshBoards();
    }

    public void fellIntoVoid(UUID id) {
        switch (match.phase()) {
            case WAITING, STARTING -> stage.teleport(id, match.arena().waitingSpawn());
            case PLAYING -> {
                if (match.isAlive(id)) {
                    die(id, true);
                } else {
                    stage.teleport(id, match.arena().spectator());
                }
            }
            case ENDING -> stage.teleport(id, match.arena().spectator());
        }
    }

    public void checkEliminations() {
        state.standingsChanged();
        for (TeamColor color : match.teams().keySet()) {
            if (match.status(color).isEliminated() && announcedEliminations.add(color)) {
                broadcast(messages.get("team-eliminated", "team-color", Colors.translate(color.color()), "team", match.teamName(color)));
                online().forEach(id -> stage.sound(id, settings.sounds().teamEliminated()));
            }
        }
        if (match.phase() == MatchPhase.PLAYING && match.isOver()) {
            end(match.winner());
        }
    }

    public boolean mayBreak(UUID id, int x, int y, int z) {
        if (!environment.rules().breakOnlyPlaced() || match.wasPlaced(x, y, z)) {
            return true;
        }
        stage.message(id, messages.get("cannot-break"));
        return false;
    }

    public boolean pauseCountdown() {
        boolean paused = match.pause();
        if (paused) {
            refreshBoards();
        }
        return paused;
    }

    public boolean forceStart(int seconds) {
        Optional<Change> change = match.forceStart(seconds);
        change.ifPresent(this::countdownChanged);
        refreshBoards();
        return change.isPresent();
    }

    public boolean forceEnd() {
        if (!match.canForceEnd()) {
            return false;
        }
        end(match.winner());
        return true;
    }

    public void end(Optional<TeamColor> winner) {
        match.end();
        state.ending();
        List<UUID> audience = online();
        for (UUID id : audience) {
            stage.sound(id, settings.sounds().matchEnd());
            stage.sound(id, settings.sounds().matchEndFirework());
            if (isSpectator(id)) {
                stage.spectatorItems(id, settings.spectatorHotbar().spectatorItems());
            }
        }
        winner.ifPresent(team -> celebrate(team, audience));
        List<String> lines = result(winner);
        online().forEach(id -> stage.centered(id, lines));
        state.ended(winner);
        refreshBoards();
    }

    public void refreshBoards() {
        String title = Colors.translate(board.title());
        for (UUID id : online()) {
            if (!boards.contains(id)) {
                continue;
            }
            List<String> lines = board.lines(match, id, state.upcoming(), state.placeholders(match.player(id))).stream().map(Colors::translate).toList();
            stage.board(id, title, lines);
        }
    }

    public void refreshVisibility() {
        stage.visibility(online(), this::isSpectator);
    }

    public void forget(UUID id) {
        state.left(id);
        stage.knockback(id, Optional.empty());
        boards.remove(id);
        stage.dropBoard(id);
    }

    public Area area() {
        return state.area().orElseGet(this::spawnArea);
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (UUID id : online()) {
            forget(id);
            stage.teleportOut(id);
            stage.refreshNameTag(id);
        }
        state.closed();
        List.copyOf(returnable).forEach(this::forgetReturn);
        onClose.accept(this);
    }

    public boolean isClosed() {
        return closed;
    }

    public static String clock(int seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private void leaveBeforeStart(Participant player) {
        Change change = match.leaveBeforeStart(player.id());
        forget(player.id());
        broadcast(messages.get("room-left", "player", stage.rankColored(player.id(), player.name()),
            "players", String.valueOf(match.players().size()), "max", String.valueOf(match.mode().maxPlayers())));
        countdownChanged(change);
        refreshBoards();
    }

    private void start() {
        Instant now = environment.clock().instant();
        List<UUID> watching = match.joinOrder().stream().filter(control.spectators()::contains).toList();
        watching.forEach(match::forget);
        List<List<UUID>> parties = control.parties(environment.parties().groupsOf(match.joinOrder()));
        match.start(parties, control.preferences(environment.preferences().preferred(match.joinOrder())), environment.random(), now);
        Set<UUID> split = match.splitParties(parties);
        match.unrank(split);
        if (!control.ranked()) {
            match.unrank(match.joinOrder());
        }
        match.protectAll(now.plusSeconds(settings.timings().respawnProtectionSeconds()));
        state.starting();
        for (UUID id : online()) {
            match.player(id).ifPresent(this::spawn);
            stage.refreshNameTag(id);
        }
        match.teams().keySet().forEach(color -> {
            if (match.status(color).isEliminated()) {
                announcedEliminations.add(color);
            }
        });
        List<Point> corners = match.arena().waitingArea();
        if (corners.size() >= 2) {
            stage.erase(corners.get(0), corners.get(1));
        }
        watching.forEach(this::watch);
        state.started();
        refreshBoards();
        messages.lines("match-started").forEach(this::broadcast);
        if (!split.isEmpty()) {
            messages.lines("party-split").forEach(this::broadcast);
        }
        online().forEach(id -> stage.sound(id, settings.sounds().matchStart()));
    }

    private void playingSecond() {
        match.tick();
        state.playing();
        if (match.phase() != MatchPhase.PLAYING) {
            return;
        }
        Instant now = environment.clock().instant();
        Set<UUID> present = new HashSet<>(online());
        for (Participant player : List.copyOf(match.players())) {
            if (player.state() == State.RESPAWNING && present.contains(player.id())) {
                int left = player.respawnAt() - match.elapsed();
                if (left <= 0) {
                    match.respawned(player, now.plusSeconds(settings.timings().respawnProtectionSeconds()));
                    spawn(player);
                    String respawned = messages.get("respawned");
                    stage.message(player.id(), respawned);
                    stage.title(player.id(), Title.builder().subtitle(respawned).timings(0, 20, 10).build());
                } else {
                    respawnCountdown(player, left);
                }
            } else if (player.state() == State.DISCONNECTED && player.disconnectedUntil().isBefore(now)) {
                match.eliminate(player);
                Death death = new Death(player, Optional.empty(), true, Cause.TIMEOUT);
                state.died(death);
                announce(death);
                checkEliminations();
            }
        }
        if (match.phase() == MatchPhase.PLAYING) {
            refreshBoards();
        }
    }

    private void spawn(Participant player) {
        stage.prepare(player.id(), Look.PLAYER);
        stage.teleport(player.id(), match.arena().spawn(player.team()));
        state.spawned(player);
        refreshVisibility();
    }

    private void ghost(UUID id, boolean items) {
        stage.prepare(id, Look.SPECTATOR);
        if (items) {
            stage.spectatorItems(id, settings.spectatorHotbar().spectatorItems());
        }
        refreshVisibility();
    }

    private void announce(Death death) {
        String victim = coloredName(death.victim());
        Optional<String> killer = death.killer().map(this::coloredName);
        String text = state.deathMessage(death, victim, killer).orElseGet(() -> killer
            .map(name -> messages.get("kill", "killer", name, "victim", victim))
            .orElseGet(() -> messages.get("death", "victim", victim)));
        if (!text.isBlank()) {
            broadcast(text);
        }
    }

    private void celebrate(TeamColor team, List<UUID> audience) {
        for (UUID id : audience) {
            if (watchers.contains(id)) {
                continue;
            }
            boolean won = match.player(id).map(Participant::team).filter(team::equals).isPresent();
            if (won && !isSpectator(id)) {
                stage.spectatorItems(id, settings.spectatorHotbar().winnerItems());
            }
            stage.endTitle(id, won);
        }
        List<Player> viewers = new ArrayList<>();
        audience.forEach(id -> stage.player(id).ifPresent(viewers::add));
        match.celebrant(team, new HashSet<>(audience), environment.random())
            .flatMap(stage::player)
            .ifPresent(celebrant -> environment.events().victory(celebrant, viewers, area()));
    }

    private List<String> result(Optional<TeamColor> winner) {
        String time = clock(match.elapsed());
        if (winner.isEmpty()) {
            return messages.lines("result-draw", "time", time);
        }
        List<String> lines = new ArrayList<>();
        for (String line : messages.lines("result", "team-color", Colors.translate(winner.get().color()), "team", match.teamName(winner.get()), "time", time)) {
            int start = line.indexOf("{top-");
            int finish = start < 0 ? -1 : line.indexOf('}', start);
            if (finish > start) {
                lines.addAll(rank(line.substring(start + "{top-".length(), finish)));
            } else {
                lines.add(line);
            }
        }
        return lines;
    }

    private List<String> rank(String ranking) {
        Map<UUID, Integer> scores = new HashMap<>();
        match.players().forEach(player -> scores.put(player.id(), state.score(ranking, player)));
        List<Ranking.Entry> top = Ranking.top(scores, match.joinOrder());
        if (top.isEmpty()) {
            return messages.lines("result-nobody");
        }
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < top.size(); index++) {
            Ranking.Entry entry = top.get(index);
            String name = match.player(entry.player()).map(Participant::name).orElse("?");
            lines.addAll(messages.lines("result-rank-" + (index + 1), "player", stage.rankColored(entry.player(), name), "score", String.valueOf(entry.score())));
        }
        return lines;
    }

    private void countdownChanged(Change change) {
        if (change == Change.STARTED || change == Change.SHORTENED) {
            match.countdown().remaining().ifPresent(remaining -> settings.countdownTitles().at(remaining).ifPresent(text -> countdownTitle(text, remaining)));
        }
    }

    private void countdownTitle(String text, int remaining) {
        Title title = Title.builder().title(Colors.translate(text)).subtitle("").timings(0, 20, 20).build();
        String message = remaining == 1 ? messages.get("countdown-one") : messages.get("countdown", "seconds", String.valueOf(remaining));
        online().forEach(id -> {
            stage.message(id, message);
            stage.title(id, title);
            stage.sound(id, settings.sounds().countdown());
        });
    }

    private void respawnCountdown(Participant player, int left) {
        String text = left == 1 ? messages.get("respawn-subtitle-one") : messages.get("respawn-subtitle", "seconds", String.valueOf(left));
        stage.message(player.id(), text);
        stage.title(player.id(), Title.builder().title(messages.get("died-title")).subtitle(text).timings(0, 25, 0).build());
    }

    private Point floor(Participant player) {
        return player.team() == null ? match.arena().spectator() : match.arena().spawn(player.team());
    }

    private void rememberReturn(UUID id) {
        returnable.add(id);
        stage.rememberReturn(id, Duration.ofSeconds(settings.timings().rejoinSeconds()));
    }

    private void forgetReturn(UUID id) {
        if (returnable.remove(id)) {
            stage.forgetReturn(id);
        }
    }

    private void forgetLostReturns() {
        Instant now = environment.clock().instant();
        List.copyOf(returnable).stream().filter(id -> !canReturn(id, now)).forEach(this::forgetReturn);
    }

    private Area spawnArea() {
        List<Point> points = new ArrayList<>();
        match.arena().teamColors().forEach(color -> points.add(match.arena().spawn(color)));
        if (points.isEmpty()) {
            points.add(match.arena().spectator());
        }
        double minX = points.stream().mapToDouble(Point::x).min().orElseThrow();
        double minZ = points.stream().mapToDouble(Point::z).min().orElseThrow();
        double maxX = points.stream().mapToDouble(Point::x).max().orElseThrow();
        double maxZ = points.stream().mapToDouble(Point::z).max().orElseThrow();
        return new Area(match.cell().toWorldX(minX) - AREA_MARGIN, match.cell().toWorldZ(minZ) - AREA_MARGIN,
            match.cell().toWorldX(maxX) + AREA_MARGIN, match.cell().toWorldZ(maxZ) + AREA_MARGIN);
    }
}
