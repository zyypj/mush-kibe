package br.com.muxe.core.common.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PendingPromptsTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-26T12:00:00Z"));
    private final PendingPrompts<String> prompts = new PendingPrompts<>(clock);
    private final UUID player = UUID.randomUUID();

    @Test
    void theFirstAnswerTakesThePrompt() {
        prompts.open(player, Duration.ofSeconds(30), "code");

        assertThat(prompts.isWaiting(player)).isTrue();
        assertThat(prompts.answer(player)).contains("code");
        assertThat(prompts.answer(player)).isEmpty();
        assertThat(prompts.isWaiting(player)).isFalse();
    }

    @Test
    void playersWithoutAPromptAreIgnored() {
        assertThat(prompts.isWaiting(player)).isFalse();
        assertThat(prompts.answer(player)).isEmpty();
    }

    @Test
    void anAnswerAfterTheDeadlineIsNotTaken() {
        prompts.open(player, Duration.ofSeconds(30), "code");

        clock.advance(Duration.ofSeconds(30));

        assertThat(prompts.isWaiting(player)).isFalse();
        assertThat(prompts.answer(player)).isEmpty();
        assertThat(prompts.expire()).containsExactly("code");
        assertThat(prompts.expire()).isEmpty();
    }

    @Test
    void expireOnlyReturnsPromptsPastTheirDeadline() {
        UUID other = UUID.randomUUID();
        prompts.open(player, Duration.ofSeconds(10), "short");
        prompts.open(other, Duration.ofSeconds(60), "long");

        clock.advance(Duration.ofSeconds(11));

        assertThat(prompts.expire()).containsExactly("short");
        assertThat(prompts.isWaiting(other)).isTrue();
    }

    @Test
    void aNewPromptReplacesTheOldOne() {
        prompts.open(player, Duration.ofSeconds(30), "first");
        prompts.open(player, Duration.ofSeconds(30), "second");

        assertThat(prompts.answer(player)).contains("second");
    }

    @Test
    void cancelRemovesThePromptEvenAfterItsDeadline() {
        prompts.open(player, Duration.ofSeconds(5), "code");
        clock.advance(Duration.ofSeconds(6));

        assertThat(prompts.cancel(player)).contains("code");
        assertThat(prompts.expire()).isEmpty();
    }

    @Test
    void theTimeoutMustBePositive() {
        assertThatThrownBy(() -> prompts.open(player, Duration.ZERO, "code")).isInstanceOf(IllegalArgumentException.class);
    }
}
