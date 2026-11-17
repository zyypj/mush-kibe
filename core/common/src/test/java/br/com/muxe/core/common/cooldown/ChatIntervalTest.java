package br.com.muxe.core.common.cooldown;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatIntervalTest {

    private static final Duration NORMAL = Duration.ofSeconds(3);
    private static final Duration FAST = Duration.ofSeconds(1);

    private final MutableClock clock = MutableClock.startingAt("2026-11-17T12:00:00Z");
    private final ChatInterval interval = new ChatInterval(clock);
    private final UUID member = UUID.randomUUID();
    private final UUID ultraPlus = UUID.randomUUID();

    @Test
    void membersWaitThreeSecondsAndUltraPlusOneSecondBetweenMessages() {
        assertThat(interval.speak(member, NORMAL)).isEmpty();
        assertThat(interval.speak(ultraPlus, FAST)).isEmpty();

        clock.advance(Duration.ofMillis(1_200));

        assertThat(interval.speak(member, NORMAL)).contains(Duration.ofMillis(1_800));
        assertThat(interval.speak(ultraPlus, FAST)).isEmpty();

        clock.advance(Duration.ofMillis(1_800));

        assertThat(interval.speak(member, NORMAL)).isEmpty();
    }

    @Test
    void aBlockedMessageDoesNotRestartTheWaitAndNoIntervalNeverBlocks() {
        interval.speak(member, NORMAL);
        clock.advance(Duration.ofSeconds(2));
        interval.speak(member, NORMAL);
        clock.advance(Duration.ofSeconds(1));

        assertThat(interval.speak(member, NORMAL)).isEmpty();
        assertThat(interval.speak(member, Duration.ZERO)).isEmpty();
        assertThat(interval.speak(member, Duration.ZERO)).isEmpty();
    }
}
