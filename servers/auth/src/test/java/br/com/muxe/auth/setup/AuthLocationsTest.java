package br.com.muxe.auth.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.location.LocationSlot;
import br.com.muxe.core.bukkit.location.StoredLocation;
import java.util.Optional;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class AuthLocationsTest {

    private final MemorySlot spawn = new MemorySlot();
    private final MemorySlot code = new MemorySlot();
    private final AuthLocations locations = new AuthLocations(spawn, code, 4);
    private final Location eyes = new Location(null, 0, 101.62, 0, 0f, 0f);

    @Test
    void withoutConfigurationTheCodeAppearsInFrontOfTheEyes() {
        Location anchor = locations.challengeAnchor(eyes, 0.84);

        assertThat(anchor.getZ()).isCloseTo(4, within(1e-9));
        assertThat(anchor.getY()).isCloseTo(101.2, within(1e-9));
    }

    @Test
    void aConfiguredCodeFacesThePlayer() {
        code.set(new Location(null, 6, 102, 0));

        Location anchor = locations.challengeAnchor(eyes, 0.84);

        assertThat(anchor.getX()).isEqualTo(6);
        assertThat(anchor.getYaw()).isCloseTo(270f, within(0.01f));
    }

    @Test
    void spawnEyesAreAboveTheConfiguredSpawn() {
        spawn.set(new Location(null, 10, 64, 10, 90f, 0f));

        Location spawnEyes = locations.spawnEyes(eyes);

        assertThat(spawnEyes.getY()).isCloseTo(64 + AuthLocations.EYE_HEIGHT, within(1e-9));
        assertThat(spawnEyes.getYaw()).isEqualTo(90f);
    }

    @Test
    void spawnEyesFallBackToThePlayerWithoutASpawn() {
        assertThat(locations.spawnEyes(eyes)).isSameAs(eyes);
        assertThat(locations.spawnLocation()).isEmpty();
    }

    private static final class MemorySlot implements LocationSlot {

        private Location location;

        @Override
        public Optional<StoredLocation> stored() {
            return Optional.ofNullable(location).map(value -> new StoredLocation("world", value.getX(), value.getY(), value.getZ(), value.getYaw(), value.getPitch()));
        }

        @Override
        public Optional<Location> location() {
            return Optional.ofNullable(location).map(Location::clone);
        }

        @Override
        public void set(Location value) {
            location = value.clone();
        }

        @Override
        public void clear() {
            location = null;
        }
    }
}
