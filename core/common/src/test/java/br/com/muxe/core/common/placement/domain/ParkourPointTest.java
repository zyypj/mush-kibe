package br.com.muxe.core.common.placement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParkourPointTest {

    @Test
    void idsNameTheStartTheEndAndEachCheckpoint() {
        assertThat(ParkourPoint.start().id()).isEqualTo("start");
        assertThat(ParkourPoint.end().id()).isEqualTo("end");
        assertThat(ParkourPoint.checkpoint(3).id()).isEqualTo("checkpoint-3");
    }

    @Test
    void idsAreReadBack() {
        assertThat(ParkourPoint.parse("start")).contains(ParkourPoint.start());
        assertThat(ParkourPoint.parse("END")).contains(ParkourPoint.end());
        assertThat(ParkourPoint.parse("checkpoint-12")).contains(ParkourPoint.checkpoint(12));
        assertThat(ParkourPoint.parse("checkpoint-0")).isEmpty();
        assertThat(ParkourPoint.parse("checkpoint-x")).isEmpty();
        assertThat(ParkourPoint.parse("meio")).isEmpty();
        assertThat(ParkourPoint.parse(null)).isEmpty();
    }

    @Test
    void checkpointsStartAtOne() {
        assertThatThrownBy(() -> ParkourPoint.checkpoint(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aGroupFindsItsPointsAndAPointMoves() {
        Position spawn = new Position("lobby", 28.5, 34, -8.5, 0, 0);
        ParkourPointPlacement start = new ParkourPointPlacement("lobby", ParkourPoint.start(), spawn);
        Placements placements = new Placements(List.of(), List.of(), List.of(), List.of(), List.of(start));

        assertThat(start.id()).isEqualTo("start");
        assertThat(start.kind()).isEqualTo(PlacementKind.PARKOUR);
        assertThat(placements.parkourPoint(ParkourPoint.start())).contains(start);
        assertThat(placements.parkourPoint(ParkourPoint.end())).isEmpty();
        assertThat(start.movedTo(new Position("lobby", 1, 2, 3, 0, 0)).point()).isEqualTo(ParkourPoint.start());
    }
}
