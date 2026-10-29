package br.com.muxe.core.common.permission.editor;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditorSessionsTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-28T15:00:00Z"));
    private final InMemoryExpiringStore store = new InMemoryExpiringStore(clock);
    private final EditorSessions sessions = new EditorSessions(store, clock, Duration.ofMinutes(30));
    private final UUID admin = UUID.randomUUID();

    @Test
    void aLinkOpensASessionForWhoAskedForIt() {
        String token = sessions.open(admin, "tadeu");

        assertThat(token).hasSizeGreaterThanOrEqualTo(40).matches("[A-Za-z0-9_-]+");
        assertThat(sessions.find(token)).contains(new EditorSession(admin, "tadeu", clock.instant()));
    }

    @Test
    void theStoreNeverKeepsTheTokenItself() {
        String token = sessions.open(admin, "tadeu");

        assertThat(store.get("permissions:editor:" + token)).isEmpty();
    }

    @Test
    void sessionsExpireAndCanBeClosed() {
        String expiring = sessions.open(admin, "tadeu");
        String closing = sessions.open(admin, "tadeu");

        sessions.close(closing);
        clock.advance(Duration.ofMinutes(30));

        assertThat(sessions.find(expiring)).isEmpty();
        assertThat(sessions.find(closing)).isEmpty();
    }

    @Test
    void wrongTokensFindNothing() {
        sessions.open(admin, "tadeu");

        assertThat(sessions.find("errado")).isEmpty();
        assertThat(sessions.find("")).isEmpty();
        assertThat(sessions.find(null)).isEmpty();
        assertThat(sessions.find("x".repeat(500))).isEmpty();
    }
}
