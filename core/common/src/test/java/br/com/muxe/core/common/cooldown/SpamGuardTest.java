package br.com.muxe.core.common.cooldown;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpamGuardTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final SpamGuard guard = new SpamGuard(clock, 3, Duration.ofSeconds(1), Duration.ofSeconds(3));
    private final UUID player = UUID.randomUUID();

    @Test
    void threeQuickClicksStartTheCooldown() {
        assertThat(guard.use(player).allowed()).isTrue();
        clock.advance(Duration.ofMillis(200));
        assertThat(guard.use(player).allowed()).isTrue();
        clock.advance(Duration.ofMillis(200));
        assertThat(guard.use(player).allowed()).isTrue();

        clock.advance(Duration.ofMillis(100));
        SpamDecision blocked = guard.use(player);

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.remaining()).isEqualTo(Duration.ofMillis(2900));
    }

    @Test
    void slowClicksNeverTriggerTheCooldown() {
        for (int click = 0; click < 10; click++) {
            assertThat(guard.use(player).allowed()).isTrue();
            clock.advance(Duration.ofMillis(600));
        }
    }

    @Test
    void theCooldownEndsAfterItsDuration() {
        guard.use(player);
        guard.use(player);
        guard.use(player);

        clock.advance(Duration.ofSeconds(3));

        assertThat(guard.use(player).allowed()).isTrue();
    }

    @Test
    void playersAreGuardedSeparately() {
        guard.use(player);
        guard.use(player);
        guard.use(player);

        assertThat(guard.use(UUID.randomUUID()).allowed()).isTrue();
    }

    @Test
    void forgettingAPlayerClearsTheCooldown() {
        guard.use(player);
        guard.use(player);
        guard.use(player);

        guard.forget(player);

        assertThat(guard.use(player).allowed()).isTrue();
    }

    @Test
    void remainingTimeIsShownWithOneDecimalAndAComma() {
        assertThat(CooldownFormat.seconds(Duration.ofMillis(2900))).isEqualTo("2,9");
        assertThat(CooldownFormat.seconds(Duration.ofMillis(2901))).isEqualTo("3,0");
        assertThat(CooldownFormat.seconds(Duration.ofMillis(30))).isEqualTo("0,1");
        assertThat(CooldownFormat.seconds(Duration.ZERO)).isEqualTo("0,1");
    }
}
