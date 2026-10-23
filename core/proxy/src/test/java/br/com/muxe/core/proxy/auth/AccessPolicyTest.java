package br.com.muxe.core.proxy.auth;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.server.ServerIdentity;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.server.directory.ServerDirectory;
import br.com.muxe.core.common.server.directory.ServerStatus;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    private final Clock clock = Clock.systemUTC();
    private final ServerDirectory directory = new ServerDirectory(clock, Duration.ofMinutes(1));
    private final AuthenticationGate gate = new AuthenticationGate();
    private final AccessPolicy policy = new AccessPolicy(gate, directory, "auth");
    private final UUID player = UUID.randomUUID();

    @Test
    void unauthenticatedPlayersOnlyReachAuthServers() {
        report("auth-1", "auth", ServerKind.AUTH);
        report("lobby-1", "lobby", ServerKind.LOBBY);

        assertThat(policy.mayConnect(player, "auth-1")).isTrue();
        assertThat(policy.mayConnect(player, "lobby-1")).isFalse();
        assertThat(policy.mayConnect(player, "unknown-1")).isFalse();
    }

    @Test
    void authenticatedPlayersReachEveryServer() {
        report("lobby-1", "lobby", ServerKind.LOBBY);
        gate.authenticate(player);

        assertThat(policy.mayConnect(player, "lobby-1")).isTrue();
    }

    @Test
    void forgettingAPlayerClosesTheGateAgain() {
        report("lobby-1", "lobby", ServerKind.LOBBY);
        gate.authenticate(player);

        gate.forget(player);

        assertThat(policy.mayConnect(player, "lobby-1")).isFalse();
    }

    @Test
    void onlyAuthServersAreTrustedToAuthenticatePlayers() {
        report("auth-1", "auth", ServerKind.AUTH);
        report("lobby-1", "lobby", ServerKind.LOBBY);

        assertThat(policy.isTrustedAuthOrigin("auth-1")).isTrue();
        assertThat(policy.isTrustedAuthOrigin("lobby-1")).isFalse();
        assertThat(policy.isTrustedAuthOrigin("ghost-1")).isFalse();
    }

    private void report(String id, String group, ServerKind kind) {
        directory.update(new ServerStatus(new ServerIdentity(id, group, kind), 0, 100, true, clock.instant(), clock.instant()));
    }
}
