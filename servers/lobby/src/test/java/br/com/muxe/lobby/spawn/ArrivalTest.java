package br.com.muxe.lobby.spawn;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class ArrivalTest {

    private final Location spawn = new Location(null, 10.5, 64, -3.5, 90f, 0f);

    @Test
    void playersWhoCanFlyArriveAboveTheSpawnAlreadyFlying() {
        Arrival arrival = Arrival.at(spawn, true, 5);

        assertThat(arrival.flying()).isTrue();
        assertThat(arrival.location().getY()).isEqualTo(69);
        assertThat(arrival.location().getX()).isEqualTo(10.5);
        assertThat(arrival.location().getYaw()).isEqualTo(90f);
    }

    @Test
    void everyoneElseArrivesOnTheSpawn() {
        Arrival arrival = Arrival.at(spawn, false, 5);

        assertThat(arrival.flying()).isFalse();
        assertThat(arrival.location()).isEqualTo(spawn);
    }

    @Test
    void theSpawnItselfIsNeverChanged() {
        Arrival.at(spawn, true, 5);

        assertThat(spawn.getY()).isEqualTo(64);
    }
}
