package br.com.muxe.core.common.buildmode;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.testing.InMemoryExpiringStore;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuildStateTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-24T12:00:00Z"));
    private final InMemoryExpiringStore store = new InMemoryExpiringStore(clock);
    private final BuildState state = new BuildState(store, Duration.ofHours(6));
    private final UUID builder = UUID.randomUUID();

    @Test
    void buildModeIsSharedThroughTheStore() {
        BuildState otherServer = new BuildState(store, Duration.ofHours(6));

        state.turnOn(builder);

        assertThat(otherServer.isOn(builder)).isTrue();
        assertThat(store.get("build:" + builder)).isPresent();
    }

    @Test
    void turningOffEndsItEverywhere() {
        state.turnOn(builder);

        state.turnOff(builder);

        assertThat(state.isOn(builder)).isFalse();
    }

    @Test
    void aForgottenBuildModeExpires() {
        state.turnOn(builder);

        clock.advance(Duration.ofHours(6).plusSeconds(1));

        assertThat(state.isOn(builder)).isFalse();
    }

    @Test
    void turningOnAgainRenewsTheExpiry() {
        state.turnOn(builder);
        clock.advance(Duration.ofHours(5));

        state.turnOn(builder);
        clock.advance(Duration.ofHours(5));

        assertThat(state.isOn(builder)).isTrue();
    }

    @Test
    void playersStartOutsideBuildMode() {
        assertThat(state.isOn(UUID.randomUUID())).isFalse();
    }
}
