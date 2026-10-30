package br.com.muxe.minigame.engine.match;

import br.com.muxe.minigame.engine.mode.GameMode;
import java.util.OptionalInt;

public final class StartCountdown {

    public static final int AT_MINIMUM = 30;
    public static final int WHEN_FULL = 10;

    private final GameMode mode;
    private final int atMinimum;
    private final int whenFull;
    private int remaining = -1;

    public StartCountdown(GameMode mode) {
        this(mode, AT_MINIMUM, WHEN_FULL);
    }

    public StartCountdown(GameMode mode, int atMinimum, int whenFull) {
        if (whenFull < 1 || atMinimum < whenFull) {
            throw new IllegalArgumentException("Invalid countdown " + atMinimum + "/" + whenFull);
        }
        this.mode = mode;
        this.atMinimum = atMinimum;
        this.whenFull = whenFull;
    }

    public Change players(int count) {
        if (count < mode.minPlayers()) {
            if (remaining < 0) {
                return Change.NONE;
            }
            remaining = -1;
            return Change.CANCELLED;
        }
        boolean full = count >= mode.maxPlayers();
        if (remaining < 0) {
            remaining = full ? whenFull : atMinimum;
            return Change.STARTED;
        }
        if (full && remaining > whenFull) {
            remaining = whenFull;
            return Change.SHORTENED;
        }
        return Change.NONE;
    }

    public OptionalInt remaining() {
        return remaining < 0 ? OptionalInt.empty() : OptionalInt.of(remaining);
    }

    public boolean isRunning() {
        return remaining >= 0;
    }

    public Second tick() {
        if (remaining < 0) {
            return new Second(false, -1);
        }
        remaining--;
        if (remaining <= 0) {
            remaining = -1;
            return new Second(true, 0);
        }
        return new Second(false, remaining);
    }

    public enum Change {
        NONE,
        STARTED,
        SHORTENED,
        CANCELLED
    }

    public record Second(boolean finished, int remaining) {
    }
}
