package br.com.muxe.core.common.parkour.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
import br.com.muxe.core.common.placement.domain.Position;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParkourCourseTest {

    private final ParkourPointPlacement start = point(ParkourPoint.start(), new Position("lobby", 28.5, 34, -8.5, 0, 0));
    private final ParkourPointPlacement first = point(ParkourPoint.checkpoint(1), new Position("lobby", -36.5, 86, 31.5, 0, 0));
    private final ParkourPointPlacement third = point(ParkourPoint.checkpoint(3), new Position("lobby", 42.5, 77, 54.5, 0, 0));
    private final ParkourPointPlacement end = point(ParkourPoint.end(), new Position("lobby", 48.5, 118, -73.5, 0, 0));

    @Test
    void aCourseNeedsAStartAndAnEnd() {
        assertThat(ParkourCourse.from(List.of(start, first))).isEmpty();
        assertThat(ParkourCourse.from(List.of(first, end))).isEmpty();
        assertThat(ParkourCourse.from(List.of(end, start))).hasValueSatisfying(course -> assertThat(course.checkpoints()).isEmpty());
    }

    @Test
    void checkpointsFollowTheirNumbers() {
        ParkourCourse course = ParkourCourse.from(List.of(third, end, first, start)).orElseThrow();

        assertThat(course.checkpoints()).containsExactly(first, third);
        assertThat(course.checkpointIndex(ParkourPoint.checkpoint(3))).isEqualTo(1);
        assertThat(course.checkpointIndex(ParkourPoint.checkpoint(2))).isEqualTo(-1);
    }

    @Test
    void aPointIsFoundByTheBlockItIsIn() {
        ParkourCourse course = ParkourCourse.from(List.of(start, first, end)).orElseThrow();

        assertThat(course.at("lobby", 28, 34, -9)).contains(start);
        assertThat(course.at("lobby", -37, 86, 31)).contains(first);
        assertThat(course.at("lobby", 28, 35, -9)).isEmpty();
        assertThat(course.at("bedwars", 28, 34, -9)).isEmpty();
    }

    private static ParkourPointPlacement point(ParkourPoint point, Position position) {
        return new ParkourPointPlacement("lobby", point, position);
    }
}
