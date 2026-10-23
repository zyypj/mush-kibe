package br.com.muxe.minigame.engine.match;

import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Match {

    private final UUID id;
    private final GameMode mode;
    private final String arenaId;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private MatchState state = MatchState.WAITING;

    Match(UUID id, GameMode mode, String arenaId) {
        this.id = Objects.requireNonNull(id, "id");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
    }

    public UUID id() {
        return id;
    }

    public GameMode mode() {
        return mode;
    }

    public String arenaId() {
        return arenaId;
    }

    public MatchState state() {
        return state;
    }

    public Set<UUID> participants() {
        return Collections.unmodifiableSet(participants);
    }

    public boolean isFull() {
        return participants.size() >= mode.maxPlayers();
    }

    public boolean hasMinimumPlayers() {
        return participants.size() >= mode.minPlayers();
    }

    public boolean acceptsPlayers() {
        return state.acceptsPlayers() && !isFull();
    }

    void transitionTo(MatchState next) {
        if (!state.canTransitionTo(next)) {
            throw new IllegalTransitionException("Match " + id, state, next);
        }
        state = next;
    }

    void add(UUID playerId) {
        participants.add(playerId);
    }

    void remove(UUID playerId) {
        participants.remove(playerId);
    }
}
