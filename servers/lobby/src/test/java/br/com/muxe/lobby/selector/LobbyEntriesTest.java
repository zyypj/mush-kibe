package br.com.muxe.lobby.selector;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LobbyEntriesTest {

    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");

    @Test
    void lobbiesAreNumberedFromTheirServerId() {
        assertThat(LobbyNumber.of("lobby-4")).hasValue(4);
        assertThat(LobbyNumber.of("lobby-12")).hasValue(12);
        assertThat(LobbyNumber.of("lobby")).isEmpty();
        assertThat(LobbyNumber.of("lobby-x")).isEmpty();
    }

    @Test
    void lobbiesAreListedInNumberOrderAndTheCurrentOneIsMarked() {
        List<LobbyEntry> entries = LobbyEntries.from(List.of(status("lobby-10", 3, 60), status("lobby-2", 32, 60), status("lobby-1", 5, 60)), "lobby-2");

        assertThat(entries).extracting(LobbyEntry::serverId).containsExactly("lobby-1", "lobby-2", "lobby-10");
        assertThat(entries).extracting(LobbyEntry::current).containsExactly(false, true, false);
        assertThat(entries.get(1).label()).isEqualTo("2");
        assertThat(entries.get(1).online()).isEqualTo(32);
        assertThat(entries.get(1).max()).isEqualTo(60);
    }

    @Test
    void lobbiesWithoutANumberComeLastAndUseTheirId() {
        List<LobbyEntry> entries = LobbyEntries.from(List.of(status("lobby-vip", 0, 60), status("lobby-1", 0, 60)), "lobby-1");

        assertThat(entries).extracting(LobbyEntry::serverId).containsExactly("lobby-1", "lobby-vip");
        assertThat(entries.get(1).label()).isEqualTo("lobby-vip");
    }

    @Test
    void aFullLobbyHasNoRoom() {
        assertThat(LobbyEntries.from(List.of(status("lobby-1", 60, 60)), "lobby-2").getFirst().hasRoom()).isFalse();
    }

    private static ServerStatus status(String id, int online, int max) {
        return new ServerStatus(new ServerIdentity(id, "lobby", ServerKind.LOBBY), online, max, true, NOW, NOW);
    }
}
