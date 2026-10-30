package br.com.muxe.core.bukkit.cosmetic.animation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BedFrameTest {

    @Test
    void theFootHalfPointsTowardsTheHead() {
        BedFrame north = BedFrame.ofBlock(6, 11, 17, 2);

        assertThat(north.foot()).isEqualTo(new Vec(6.5, 11, 17.5));
        assertThat(north.head()).isEqualTo(new Vec(6.5, 11, 16.5));
    }

    @Test
    void theHeadHalfFindsItsFoot() {
        BedFrame east = BedFrame.ofBlock(4, 70, 0, 3 | 8);

        assertThat(east.foot()).isEqualTo(new Vec(3.5, 70, 0.5));
        assertThat(east.forward()).isEqualTo(new Vec(1, 0, 0));
    }

    @Test
    void timelinesRunEveryStepOnItsTicks() {
        Timeline timeline = new Timeline();
        StringBuilder log = new StringBuilder();
        timeline.at(2, () -> log.append('a'));
        timeline.repeat(0, 4, 2, tick -> log.append(tick));

        for (int tick = 0; tick <= timeline.length(); tick++) {
            timeline.run(tick);
        }

        assertThat(log.toString()).isEqualTo("0a24");
        assertThat(timeline.length()).isEqualTo(4);
    }
}
