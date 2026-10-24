package br.com.muxe.auth.protection;

import br.com.muxe.auth.command.AuthCommands;
import java.util.Locale;

public final class AuthProtection {

    private AuthProtection() {
    }

    public static boolean mayInteract(boolean authenticated, boolean building) {
        return authenticated && building;
    }

    public static boolean mayRunCommand(boolean authenticated, String commandLine) {
        return authenticated || AuthCommands.ALLOWED_LABELS.contains(label(commandLine));
    }

    public static boolean fellOut(double y, int voidLevel) {
        return y < voidLevel;
    }

    static String label(String commandLine) {
        String withoutSlash = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        String label = withoutSlash.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int namespace = label.indexOf(':');
        return namespace >= 0 ? label.substring(namespace + 1) : label;
    }
}
