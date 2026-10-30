package br.com.muxe.core.bukkit.text;

import java.util.List;
import java.util.Objects;

public record ChatButton(String label, List<String> hover, String command, Action action) {

    public enum Action {
        RUN,
        SUGGEST,
        NONE
    }

    public ChatButton {
        Objects.requireNonNull(label, "label");
        hover = List.copyOf(hover);
        Objects.requireNonNull(action, "action");
        if (action != Action.NONE && (command == null || !command.startsWith("/"))) {
            throw new IllegalArgumentException("A clickable button needs a command starting with /, got: " + command);
        }
    }

    public static ChatButton run(String label, String command, String... hover) {
        return new ChatButton(label, List.of(hover), command, Action.RUN);
    }

    public static ChatButton suggest(String label, String command, String... hover) {
        return new ChatButton(label, List.of(hover), command, Action.SUGGEST);
    }

    public static ChatButton text(String label, String... hover) {
        return new ChatButton(label, List.of(hover), null, Action.NONE);
    }
}
