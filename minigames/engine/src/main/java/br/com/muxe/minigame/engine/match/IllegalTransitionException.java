package br.com.muxe.minigame.engine.match;

public final class IllegalTransitionException extends IllegalStateException {

    public IllegalTransitionException(String subject, Enum<?> from, Enum<?> to) {
        super(subject + " cannot go from " + from + " to " + to);
    }
}
