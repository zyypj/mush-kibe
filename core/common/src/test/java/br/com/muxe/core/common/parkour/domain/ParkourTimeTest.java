package br.com.muxe.core.common.parkour.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ParkourTimeTest {

    @Test
    void timesShowMinutesSecondsAndMillisecondsWithAComma() {
        assertThat(ParkourTime.format(Duration.ofMillis(82_704))).isEqualTo("1m 22,704s");
        assertThat(ParkourTime.format(Duration.ofMillis(298_902))).isEqualTo("4m 58,902s");
        assertThat(ParkourTime.format(Duration.ofMillis(194_002))).isEqualTo("3m 14,002s");
    }

    @Test
    void shortAndLongTimesDropOrAddUnits() {
        assertThat(ParkourTime.format(Duration.ofMillis(9_050))).isEqualTo("9,050s");
        assertThat(ParkourTime.format(Duration.ofMillis(3_723_004))).isEqualTo("1h 2m 3,004s");
        assertThat(ParkourTime.format(Duration.ofMillis(-5))).isEqualTo("0,000s");
    }
}
