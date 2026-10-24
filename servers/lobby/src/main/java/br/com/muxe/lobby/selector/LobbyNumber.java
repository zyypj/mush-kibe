package br.com.muxe.lobby.selector;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LobbyNumber {

    private static final Pattern TRAILING_NUMBER = Pattern.compile(".*-(\\d{1,6})");

    private LobbyNumber() {
    }

    public static OptionalInt of(String serverId) {
        Matcher matcher = TRAILING_NUMBER.matcher(serverId);
        return matcher.matches() ? OptionalInt.of(Integer.parseInt(matcher.group(1))) : OptionalInt.empty();
    }

    public static String label(String serverId) {
        OptionalInt number = of(serverId);
        return number.isPresent() ? Integer.toString(number.getAsInt()) : serverId;
    }
}
