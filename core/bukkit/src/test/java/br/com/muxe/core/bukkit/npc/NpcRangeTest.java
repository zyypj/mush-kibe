package br.com.muxe.core.bukkit.npc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NpcRangeTest {

    @Test
    void npcsAppearInsideTheShowRadius() {
        assertThat(NpcRange.decide(false, square(NpcRange.SHOW_RADIUS - 1))).isEqualTo(NpcRange.Decision.SHOW);
        assertThat(NpcRange.decide(false, square(NpcRange.SHOW_RADIUS + 1))).isEqualTo(NpcRange.Decision.KEEP);
    }

    @Test
    void shownNpcsOnlyDisappearBeyondTheHideRadius() {
        assertThat(NpcRange.decide(true, square(NpcRange.SHOW_RADIUS + 1))).isEqualTo(NpcRange.Decision.KEEP);
        assertThat(NpcRange.decide(true, square(NpcRange.HIDE_RADIUS + 1))).isEqualTo(NpcRange.Decision.HIDE);
    }

    @Test
    void npcsInAnotherWorldAreHidden() {
        assertThat(NpcRange.decide(true, Double.POSITIVE_INFINITY)).isEqualTo(NpcRange.Decision.HIDE);
        assertThat(NpcRange.decide(false, Double.POSITIVE_INFINITY)).isEqualTo(NpcRange.Decision.KEEP);
    }

    @Test
    void profileNamesAreShortStableAndUnique() {
        assertThat(Npc.profileName("skywars")).hasSizeLessThanOrEqualTo(16).isEqualTo(Npc.profileName("skywars"));
        assertThat(Npc.profileName("skywars")).isNotEqualTo(Npc.profileName("bedwars"));
        assertThat(Npc.profileId("skywars")).isEqualTo(Npc.profileId("skywars"));
    }

    private static double square(double value) {
        return value * value;
    }
}
