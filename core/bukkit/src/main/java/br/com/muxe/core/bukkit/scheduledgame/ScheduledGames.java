package br.com.muxe.core.bukkit.scheduledgame;

import br.com.muxe.core.common.store.ExpiringStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class ScheduledGames {

    public static final Duration ROOMS_TIME_TO_LIVE = Duration.ofSeconds(15);
    public static final Duration JOIN_TIME_TO_LIVE = Duration.ofSeconds(60);

    private final Map<String, ScheduledGame> games;
    private final Clock clock;
    private final ExpiringStore store;
    private final Map<String, Consumer<Player>> local = new ConcurrentHashMap<>();
    private final Map<String, Instant> announced = new HashMap<>();

    public ScheduledGames(Map<String, ScheduledGame> games, Clock clock, ExpiringStore store) {
        this.games = Map.copyOf(games);
        this.clock = clock;
        this.store = store;
    }

    public List<ScheduledGame> all() {
        return List.copyOf(games.values());
    }

    public Optional<ScheduledGame> game(String id) {
        return Optional.ofNullable(games.get(id));
    }

    public Optional<ScheduledGame> of(String game, String mode) {
        return games.values().stream().filter(found -> found.game().equals(game) && found.mode().equals(mode)).findFirst();
    }

    public boolean isOpen(String id) {
        return game(id).map(found -> found.schedule().isOpen(clock.instant())).orElse(false);
    }

    public boolean allowsNewRoom(String game, String mode) {
        return of(game, mode).map(found -> found.schedule().isOpen(clock.instant())).orElse(true);
    }

    public void roomsOpen(String id) {
        store.put(roomsKey(id), "1", ROOMS_TIME_TO_LIVE);
    }

    public boolean hasRooms(String id) {
        return store.get(roomsKey(id)).isPresent();
    }

    public void playHere(String id, Consumer<Player> join) {
        local.put(id, join);
    }

    public Optional<Consumer<Player>> here(String id) {
        return Optional.ofNullable(local.get(id));
    }

    public void rememberJoin(UUID player, String id) {
        store.put(joinKey(player), id, JOIN_TIME_TO_LIVE);
    }

    public Optional<String> takeJoin(UUID player) {
        Optional<String> id = store.get(joinKey(player));
        id.ifPresent(ignored -> store.delete(joinKey(player)));
        return id;
    }

    public synchronized List<ScheduledGame> dueAnnouncements() {
        Instant now = clock.instant();
        List<ScheduledGame> due = new ArrayList<>();
        for (ScheduledGame game : games.values()) {
            if (!game.schedule().isOpen(now)) {
                announced.remove(game.id());
                continue;
            }
            Instant last = announced.get(game.id());
            if (last == null || !now.isBefore(last.plus(game.announceEvery()))) {
                announced.put(game.id(), now);
                due.add(game);
            }
        }
        return due;
    }

    private static String roomsKey(String id) {
        return "scheduled-game:rooms:" + id;
    }

    private static String joinKey(UUID player) {
        return "scheduled-game:join:" + player;
    }
}
