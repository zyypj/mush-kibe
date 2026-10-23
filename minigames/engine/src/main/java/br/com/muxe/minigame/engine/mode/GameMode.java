package br.com.muxe.minigame.engine.mode;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public record GameMode(String id, String displayName, int minPlayers, int maxPlayers, Duration countdown) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public GameMode {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(countdown, "countdown");
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Game mode id must match " + ID.pattern() + ", got: " + id);
        }
        if (minPlayers < 1 || maxPlayers < minPlayers) {
            throw new IllegalArgumentException("Invalid player limits for " + id + ": " + minPlayers + "-" + maxPlayers);
        }
    }
}
