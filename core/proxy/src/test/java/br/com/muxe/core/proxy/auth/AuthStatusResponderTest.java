package br.com.muxe.core.proxy.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.messaging.request.RequestTimeoutException;
import br.com.muxe.core.common.network.auth.AuthStatusRequest;
import br.com.muxe.core.common.network.auth.AuthStatusResponse;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuthStatusResponderTest {

    private final TestNetwork network = new TestNetwork();
    private final TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
    private final TestNetwork.Node auth = network.node("auth-1", "auth", ServerKind.AUTH);
    private final AuthenticationGate gate = new AuthenticationGate();
    private final UUID player = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void tellsWhetherAConnectedPlayerIsAuthenticated() throws Exception {
        new AuthStatusResponder(gate, Set.of(player)::contains).register(proxy.bus());
        start();

        assertThat(ask().authenticated()).isFalse();

        gate.authenticate(player);

        assertThat(ask().authenticated()).isTrue();
    }

    @Test
    void staysSilentAboutPlayersOnAnotherProxy() {
        gate.authenticate(player);
        new AuthStatusResponder(gate, Set.<UUID>of()::contains).register(proxy.bus());
        start();

        assertThatThrownBy(this::ask)
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(RequestTimeoutException.class);
    }

    private AuthStatusResponse ask() throws Exception {
        return auth.bus()
            .request(PacketTarget.proxy(), new AuthStatusRequest(player), AuthStatusResponse.class, Duration.ofMillis(200))
            .get(2, TimeUnit.SECONDS);
    }

    private void start() {
        proxy.start();
        auth.start();
    }
}
