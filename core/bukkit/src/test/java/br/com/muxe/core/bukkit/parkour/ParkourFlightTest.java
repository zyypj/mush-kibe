package br.com.muxe.core.bukkit.parkour;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.junit.jupiter.api.Test;

class ParkourFlightTest {

    @Test
    void nobodyStartsFlyingDuringARun() {
        PlayerToggleFlightEvent takeOff = new PlayerToggleFlightEvent(null, true);

        Parkour.keepGrounded(takeOff, true);

        assertThat(takeOff.isCancelled()).isTrue();
    }

    @Test
    void landingAndFlyingOutsideARunAreAllowed() {
        PlayerToggleFlightEvent landing = new PlayerToggleFlightEvent(null, false);
        PlayerToggleFlightEvent outside = new PlayerToggleFlightEvent(null, true);

        Parkour.keepGrounded(landing, true);
        Parkour.keepGrounded(outside, false);

        assertThat(landing.isCancelled()).isFalse();
        assertThat(outside.isCancelled()).isFalse();
    }
}
