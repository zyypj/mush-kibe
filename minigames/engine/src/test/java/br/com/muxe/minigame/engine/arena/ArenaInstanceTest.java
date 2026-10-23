package br.com.muxe.minigame.engine.arena;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.minigame.engine.match.IllegalTransitionException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ArenaInstanceTest {

    private final ArenaInstance arena = new ArenaInstance("islands-1", "islands", 3);

    @Test
    void followsTheFullLifecycle() {
        arena.reserve();
        arena.startUsing();
        arena.release();
        arena.resetSucceeded();

        assertThat(arena.state()).isEqualTo(ArenaState.AVAILABLE);
    }

    @Test
    void isDisabledAfterTooManyFailedResets() {
        arena.reserve();
        arena.release();

        arena.resetFailed(false);
        arena.resetFailed(false);
        assertThat(arena.state()).isEqualTo(ArenaState.RESETTING);

        arena.resetFailed(false);
        assertThat(arena.state()).isEqualTo(ArenaState.DISABLED);
    }

    @Test
    void isDisabledImmediatelyOnPermanentFailures() {
        arena.reserve();
        arena.release();

        arena.resetFailed(true);

        assertThat(arena.state()).isEqualTo(ArenaState.DISABLED);
    }

    @Test
    void waitsLongerAfterEachConsecutiveFailure() {
        arena.reserve();
        arena.release();
        arena.resetFailed(false);
        Duration first = arena.nextRetryDelay(Duration.ofSeconds(5));
        arena.resetFailed(false);

        assertThat(first).isEqualTo(Duration.ofSeconds(5));
        assertThat(arena.nextRetryDelay(Duration.ofSeconds(5))).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void aSuccessfulResetClearsTheFailureCount() {
        arena.reserve();
        arena.release();
        arena.resetFailed(false);

        arena.resetSucceeded();

        assertThat(arena.consecutiveFailures()).isZero();
    }

    @Test
    void cannotBeReservedTwice() {
        arena.reserve();

        assertThatThrownBy(arena::reserve).isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void thePoolOnlyHandsOutAvailableArenas() {
        ArenaPool pool = new ArenaPool();
        pool.add(arena);
        pool.add(new ArenaInstance("islands-2", "islands", 3));

        assertThat(pool.reserve("islands")).map(ArenaInstance::id).contains("islands-1");
        assertThat(pool.reserve("islands")).map(ArenaInstance::id).contains("islands-2");
        assertThat(pool.reserve("islands")).isEmpty();
    }
}
