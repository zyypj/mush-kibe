package br.com.muxe.core.bukkit.command.framework.message;

import java.util.List;

public final class CommandMessages {

    public static final String NO_PERMISSION = "no-permission";
    public static final String PLAYERS_ONLY = "players-only";
    public static final String CONSOLE_ONLY = "console-only";
    public static final String USAGE = "command-usage";
    public static final String USAGE_HEADER = "command-usage-header";
    public static final String USAGE_LINE = "command-usage-line";
    public static final String INVALID_NUMBER = "command-invalid-number";
    public static final String INVALID_OPTION = "command-invalid-option";
    public static final String PLAYER_OFFLINE = "command-player-offline";
    public static final String FAILED = "command-failed";

    public static final List<String> ALL = List.of(
        NO_PERMISSION,
        PLAYERS_ONLY,
        CONSOLE_ONLY,
        USAGE,
        USAGE_HEADER,
        USAGE_LINE,
        INVALID_NUMBER,
        INVALID_OPTION,
        PLAYER_OFFLINE,
        FAILED
    );

    private CommandMessages() {
    }
}
