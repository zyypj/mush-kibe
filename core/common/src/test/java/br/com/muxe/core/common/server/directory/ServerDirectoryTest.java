package br.com.muxe.core.common.server.directory;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ServerDirectoryTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final ServerDirectory directory = new ServerDirectory(clock, Duration.ofSeconds(10));

    @Test
    void picksTheLeastLoadedServerWithRoom() {
        report("lobby-1", 50, 100, true);
        report("lobby-2", 10, 100, true);
        report("lobby-3", 0, 100, false);
        report("lobby-4", 100, 100, true);

        assertThat(directory.leastLoaded("lobby")).map(status -> status.identity().id()).contains("lobby-2");
    }

    @Test
    void forgetsServersThatStopReporting() {
        report("lobby-1", 0, 100, true);

        clock.advance(Duration.ofSeconds(11));

        assertThat(directory.find("lobby-1")).isEmpty();
        assertThat(directory.leastLoaded("lobby")).isEmpty();
    }

    @Test
    void ignoresStatusesOlderThanTheCurrentOne() {
        report("lobby-1", 30, 100, true);
        ServerStatus stale = new ServerStatus(identity("lobby-1"), 99, 100, true, clock.instant().minusSeconds(5), clock.instant());

        directory.update(stale);

        assertThat(directory.find("lobby-1")).map(ServerStatus::onlinePlayers).contains(30);
    }

    @Test
    void countsPlayersOnBackendServersOnly() {
        report("lobby-1", 30, 100, true);
        directory.update(new ServerStatus(new ServerIdentity("proxy-1", "proxy", ServerKind.PROXY), 30, 500, true, clock.instant(), clock.instant()));

        assertThat(directory.onlinePlayers()).isEqualTo(30);
    }

    private void report(String id, int online, int max, boolean joinable) {
        directory.update(new ServerStatus(identity(id), online, max, joinable, clock.instant(), clock.instant()));
    }

    private static ServerIdentity identity(String id) {
        return new ServerIdentity(id, "lobby", ServerKind.LOBBY);
    }
}
