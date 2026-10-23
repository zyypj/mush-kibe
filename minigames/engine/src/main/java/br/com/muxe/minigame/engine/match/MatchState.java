package br.com.muxe.minigame.engine.match;

import java.util.EnumSet;
import java.util.Set;

public enum MatchState {
    WAITING,
    STARTING,
    RUNNING,
    ENDING,
    CANCELLED,
    CLOSED;

    public Set<MatchState> next() {
        return switch (this) {
            case WAITING -> EnumSet.of(STARTING, CANCELLED);
            case STARTING -> EnumSet.of(WAITING, RUNNING, CANCELLED);
            case RUNNING -> EnumSet.of(ENDING, CANCELLED);
            case ENDING -> EnumSet.of(CLOSED, CANCELLED);
            case CANCELLED -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.noneOf(MatchState.class);
        };
    }

    public boolean canTransitionTo(MatchState target) {
        return next().contains(target);
    }

    public boolean acceptsPlayers() {
        return this == WAITING || this == STARTING;
    }
}
