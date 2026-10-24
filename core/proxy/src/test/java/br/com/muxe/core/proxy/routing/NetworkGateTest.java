package br.com.muxe.core.proxy.routing;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerStatus;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.proxy.config.RoutingSettings;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NetworkGateTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-24T12:00:00Z"));
    private final ServerDirectory directory = new ServerDirectory(clock, Duration.ofSeconds(10));
    private final NetworkGate gate = new NetworkGate(directory, new RoutingSettings("auth", "lobby"));

    @Test
    void theNetworkIsClosedWhileNoAuthServerIsAlive() {
        report("lobby-1", "lobby", ServerKind.LOBBY, 0, 200);

        assertThat(gate.acceptsLogins()).isFalse();
    }

    @Test
    void anAliveAuthServerOpensTheNetwork() {
        report("auth-1", "auth", ServerKind.AUTH, 0, 200);

        assertThat(gate.acceptsLogins()).isTrue();
    }

    @Test
    void aFullAuthServerStillCountsAsUp() {
        report("auth-1", "auth", ServerKind.AUTH, 200, 200);

        assertThat(gate.acceptsLogins()).isTrue();
    }

    @Test
    void anAuthServerThatStoppedReportingClosesTheNetwork() {
        report("auth-1", "auth", ServerKind.AUTH, 0, 200);

        clock.advance(Duration.ofSeconds(11));

        assertThat(gate.acceptsLogins()).isFalse();
    }

    private void report(String id, String group, ServerKind kind, int online, int max) {
        directory.update(new ServerStatus(new ServerIdentity(id, group, kind), online, max, true, clock.instant(), clock.instant()));
    }
}
