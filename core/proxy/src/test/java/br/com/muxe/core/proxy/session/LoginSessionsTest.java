package br.com.muxe.core.proxy.session;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.MutableClock;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginSessionsTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final InMemoryExpiringStore store = new InMemoryExpiringStore(clock);
    private final LoginSessions sessions = new LoginSessions(store, Duration.ofHours(2));
    private final UUID player = UUID.randomUUID();

    @Test
    void resumesFromTheSameAddressWithinTheDuration() throws UnknownHostException {
        sessions.remember(player, address("203.0.113.7"));

        clock.advance(Duration.ofMinutes(119));

        assertThat(sessions.canResume(player, address("203.0.113.7"))).isTrue();
    }

    @Test
    void expiresAfterTheDuration() throws UnknownHostException {
        sessions.remember(player, address("203.0.113.7"));

        clock.advance(Duration.ofHours(2));

        assertThat(sessions.canResume(player, address("203.0.113.7"))).isFalse();
    }

    @Test
    void refusesAnotherAddress() throws UnknownHostException {
        sessions.remember(player, address("203.0.113.7"));

        assertThat(sessions.canResume(player, address("198.51.100.9"))).isFalse();
    }

    @Test
    void refusesAnotherPlayerFromTheSameAddress() throws UnknownHostException {
        sessions.remember(player, address("203.0.113.7"));

        assertThat(sessions.canResume(UUID.randomUUID(), address("203.0.113.7"))).isFalse();
    }

    @Test
    void neverStoresTheRawAddress() throws UnknownHostException {
        sessions.remember(player, address("203.0.113.7"));

        assertThat(store.get("auth:session:" + player)).hasValueSatisfying(value -> assertThat(value).doesNotContain("203.0.113.7").hasSize(64));
    }

    @Test
    void forgettingEndsTheSession() throws UnknownHostException {
        sessions.remember(player, address("203.0.113.7"));

        sessions.forget(player);

        assertThat(sessions.canResume(player, address("203.0.113.7"))).isFalse();
    }

    private static InetAddress address(String ip) throws UnknownHostException {
        return InetAddress.getByName(ip);
    }
}
