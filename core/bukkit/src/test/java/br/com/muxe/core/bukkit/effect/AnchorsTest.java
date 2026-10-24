package br.com.muxe.core.bukkit.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class AnchorsTest {

    @Test
    void contentFacesTheViewer() {
        Location viewer = new Location(null, 0, 101.62, 0);
        Location content = new Location(null, 0, 102, 5);

        Location anchor = Anchors.lookingAt(content, viewer, 0.84);

        assertThat(anchor.getYaw()).isCloseTo(0f, within(0.01f));
        assertThat(anchor.getPitch()).isZero();
        assertThat(anchor.getY()).isCloseTo(102 - 0.42, within(1e-9));
        assertThat(anchor.getX()).isEqualTo(0);
        assertThat(anchor.getZ()).isEqualTo(5);
    }

    @Test
    void contentToTheEastIsSeenLookingEast() {
        Location viewer = new Location(null, 0, 100, 0);
        Location content = new Location(null, 5, 100, 0);

        assertThat(Anchors.lookingAt(content, viewer, 1).getYaw()).isCloseTo(270f, within(0.01f));
    }

    @Test
    void placesContentInFrontOfTheEyes() {
        Location eyes = new Location(null, 0, 100, 0, 0f, 30f);

        Location anchor = Anchors.inFrontOfEyes(eyes, 4, 1);

        assertThat(anchor.getZ()).isCloseTo(4, within(1e-9));
        assertThat(anchor.getY()).isCloseTo(99.5, within(1e-9));
    }
}
