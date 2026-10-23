package br.com.muxe.minigame.engine.match;

import br.com.muxe.core.common.result.Result;
import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class MatchService {

    private final Supplier<UUID> ids;
    private final Map<UUID, Match> matches = new LinkedHashMap<>();
    private final Map<UUID, UUID> matchByPlayer = new HashMap<>();

    public MatchService() {
        this(UUID::randomUUID);
    }

    public MatchService(Supplier<UUID> ids) {
        this.ids = ids;
    }

    public Match create(GameMode mode, String arenaId) {
        Match match = new Match(ids.get(), mode, arenaId);
        matches.put(match.id(), match);
        return match;
    }

    public Result<Match> join(UUID matchId, UUID playerId) {
        if (matchByPlayer.containsKey(playerId)) {
            return Result.failure(MatchProblems.ALREADY_IN_MATCH, "Player is already in a match");
        }
        Match match = matches.get(matchId);
        if (match == null) {
            return Result.failure(MatchProblems.NOT_FOUND, "Match " + matchId + " does not exist");
        }
        if (!match.state().acceptsPlayers()) {
            return Result.failure(MatchProblems.NOT_JOINABLE, "Match is " + match.state());
        }
        if (match.isFull()) {
            return Result.failure(MatchProblems.FULL, "Match is full");
        }
        match.add(playerId);
        matchByPlayer.put(playerId, matchId);
        if (match.state() == MatchState.WAITING && match.hasMinimumPlayers()) {
            match.transitionTo(MatchState.STARTING);
        }
        return Result.success(match);
    }

    public void leave(UUID playerId) {
        UUID matchId = matchByPlayer.remove(playerId);
        if (matchId == null) {
            return;
        }
        Match match = matches.get(matchId);
        match.remove(playerId);
        if (match.state() == MatchState.STARTING && !match.hasMinimumPlayers()) {
            match.transitionTo(MatchState.WAITING);
        }
    }

    public void start(UUID matchId) {
        require(matchId).transitionTo(MatchState.RUNNING);
    }

    public void end(UUID matchId) {
        require(matchId).transitionTo(MatchState.ENDING);
    }

    public void cancel(UUID matchId) {
        require(matchId).transitionTo(MatchState.CANCELLED);
    }

    public Collection<UUID> close(UUID matchId) {
        Match match = require(matchId);
        match.transitionTo(MatchState.CLOSED);
        List<UUID> released = List.copyOf(match.participants());
        released.forEach(matchByPlayer::remove);
        matches.remove(matchId);
        return released;
    }

    public Optional<Match> find(UUID matchId) {
        return Optional.ofNullable(matches.get(matchId));
    }

    public Optional<Match> matchOf(UUID playerId) {
        return Optional.ofNullable(matchByPlayer.get(playerId)).map(matches::get);
    }

    public Collection<Match> active() {
        return Collections.unmodifiableCollection(matches.values());
    }

    private Match require(UUID matchId) {
        Match match = matches.get(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match " + matchId + " does not exist");
        }
        return match;
    }
}
