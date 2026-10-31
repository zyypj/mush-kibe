package br.com.muxe.core.common.server.directory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class ServerDirectory {

    public static final String LOBBY_SUFFIX = "-lobby";

    private final Clock clock;
    private final Duration timeToLive;
    private final Map<String, ServerStatus> servers = new ConcurrentHashMap<>();

    public ServerDirectory(Clock clock, Duration timeToLive) {
        this.clock = clock;
        this.timeToLive = timeToLive;
    }

    public void update(ServerStatus status) {
        servers.merge(status.identity().id(), status, (current, incoming) ->
            incoming.reportedAt().isBefore(current.reportedAt()) ? current : incoming);
    }

    public void remove(String serverId) {
        servers.remove(serverId);
    }

    public Optional<ServerStatus> find(String serverId) {
        return Optional.ofNullable(servers.get(serverId)).filter(this::isAlive);
    }

    public List<ServerStatus> alive() {
        return servers.values().stream()
            .filter(this::isAlive)
            .sorted(Comparator.comparing(status -> status.identity().id()))
            .toList();
    }

    public List<ServerStatus> aliveInGroup(String group) {
        return alive().stream().filter(status -> status.identity().group().equals(group)).toList();
    }

    public Optional<ServerStatus> leastLoaded(String group) {
        return aliveInGroup(group).stream()
            .filter(ServerStatus::hasRoom)
            .min(Comparator.comparingInt(ServerStatus::onlinePlayers).thenComparing(status -> status.identity().id()));
    }

    public Optional<String> entryGroup(String game) {
        String lobby = lobbyGroup(game);
        if (leastLoaded(lobby).isPresent()) {
            return Optional.of(lobby);
        }
        return leastLoaded(game).map(status -> game);
    }

    public int gamePlayers(String game) {
        return Stream.of(game, lobbyGroup(game))
            .flatMap(group -> aliveInGroup(group).stream())
            .mapToInt(ServerStatus::onlinePlayers)
            .sum();
    }

    private static String lobbyGroup(String game) {
        return game + LOBBY_SUFFIX;
    }

    public int onlinePlayers() {
        return alive().stream()
            .filter(status -> !status.identity().isProxy())
            .mapToInt(ServerStatus::onlinePlayers)
            .sum();
    }

    public void purgeExpired() {
        servers.values().removeIf(status -> !isAlive(status));
    }

    private boolean isAlive(ServerStatus status) {
        Instant deadline = status.receivedAt().plus(timeToLive);
        return clock.instant().isBefore(deadline);
    }
}
