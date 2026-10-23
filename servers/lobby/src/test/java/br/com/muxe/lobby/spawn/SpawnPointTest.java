package br.com.muxe.lobby.spawn;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.source.MapConfigSource;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpawnPointTest {

    @Test
    void roundTripsThroughConfiguration() {
        SpawnPoint point = new SpawnPoint("world", 0.5, 64, -10.5, 90f, 15f);

        SpawnPoint loaded = SpawnPoint.from(new MapConfigSource(Map.of("spawn", point.toMap())), "spawn").orElseThrow();

        assertThat(loaded).isEqualTo(point);
    }

    @Test
    void isAbsentWhenNotConfigured() {
        assertThat(SpawnPoint.from(new MapConfigSource(Map.of()), "spawn")).isEmpty();
    }
}
