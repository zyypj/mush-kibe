package br.com.muxe.core.bukkit.location;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.source.MapConfigSource;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredLocationTest {

    @Test
    void roundTripsThroughConfiguration() {
        StoredLocation location = new StoredLocation("world", 0.5, 64, -10.5, 90f, 15f);

        StoredLocation loaded = StoredLocation.from(new MapConfigSource(Map.of("spawn", location.toMap())), "spawn").orElseThrow();

        assertThat(loaded).isEqualTo(location);
    }

    @Test
    void isAbsentWhenNotConfigured() {
        assertThat(StoredLocation.from(new MapConfigSource(Map.of()), "spawn")).isEmpty();
    }

    @Test
    void doesNotResolveInAMissingWorld() {
        StoredLocation location = new StoredLocation("gone", 0, 0, 0, 0, 0);

        assertThat(location.resolve(name -> null)).isEmpty();
    }

    @Test
    void describesItselfForStaff() {
        assertThat(new StoredLocation("world", 0.5, 100, -3, 90f, 0f).describe()).isEqualTo("world 0.5, 100.0, -3.0 (yaw 90, pitch 0)");
    }
}
