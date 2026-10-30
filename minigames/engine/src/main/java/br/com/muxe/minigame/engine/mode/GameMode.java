package br.com.muxe.minigame.engine.mode;

import java.util.Objects;
import java.util.regex.Pattern;

public record GameMode(String id, String displayName, int minPlayers, int maxPlayers, int teamSize) {

    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public GameMode {
        Objects.requireNonNull(displayName, "displayName");
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Game mode id must match " + ID.pattern() + ", got: " + id);
        }
        if (minPlayers < 1 || maxPlayers < minPlayers) {
            throw new IllegalArgumentException("Invalid player limits for " + id + ": " + minPlayers + "-" + maxPlayers);
        }
        if (teamSize < 1 || maxPlayers % teamSize != 0) {
            throw new IllegalArgumentException("Team size " + teamSize + " does not divide the " + maxPlayers + " players of " + id);
        }
    }

    public int teams() {
        return maxPlayers / teamSize;
    }

    public boolean startsFull() {
        return minPlayers == maxPlayers;
    }
}
