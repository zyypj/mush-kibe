package br.com.muxe.lobby.selector;

import br.com.muxe.core.common.server.directory.ServerStatus;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

public final class LobbyEntries {

    private static final Comparator<LobbyEntry> ORDER = Comparator
        .comparingInt((LobbyEntry entry) -> number(entry.serverId()))
        .thenComparing(LobbyEntry::serverId);

    private LobbyEntries() {
    }

    public static List<LobbyEntry> from(List<ServerStatus> lobbies, String currentServerId) {
        return lobbies.stream()
            .map(status -> new LobbyEntry(
                status.identity().id(),
                LobbyNumber.label(status.identity().id()),
                status.onlinePlayers(),
                status.maxPlayers(),
                status.joinable(),
                status.identity().id().equals(currentServerId)
            ))
            .sorted(ORDER)
            .toList();
    }

    private static int number(String serverId) {
        OptionalInt number = LobbyNumber.of(serverId);
        return number.isPresent() ? number.getAsInt() : Integer.MAX_VALUE;
    }
}
