package br.com.muxe.auth.flow;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.network.auth.AuthStatusRequest;
import br.com.muxe.core.common.network.auth.AuthStatusResponse;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProxyAuthenticationCheckTest {

    private final TestNetwork network = new TestNetwork();
    private final TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
    private final TestNetwork.Node auth = network.node("auth-1", "auth", ServerKind.AUTH);
    private final ProxyAuthenticationCheck check = new ProxyAuthenticationCheck(auth.bus(), Duration.ofMillis(200));
    private final UUID player = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void trustsTheProxyAnswer() throws Exception {
        Set<UUID> authenticated = Set.of(player);
        proxy.bus().subscribe(AuthStatusRequest.TYPE, context ->
            context.reply(new AuthStatusResponse(context.packet().playerId(), authenticated.contains(context.packet().playerId()))));
        start();

        assertThat(check.isAuthenticated(player).get(2, TimeUnit.SECONDS)).isTrue();
        assertThat(check.isAuthenticated(UUID.randomUUID()).get(2, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    void anUnansweredQuestionMeansNotAuthenticated() throws Exception {
        start();

        assertThat(check.isAuthenticated(player).get(2, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    void ignoresAnswersAboutAnotherPlayer() throws Exception {
        proxy.bus().subscribe(AuthStatusRequest.TYPE, context -> context.reply(new AuthStatusResponse(UUID.randomUUID(), true)));
        start();

        assertThat(check.isAuthenticated(player).get(2, TimeUnit.SECONDS)).isFalse();
    }

    private void start() {
        proxy.start();
        auth.start();
    }
}
