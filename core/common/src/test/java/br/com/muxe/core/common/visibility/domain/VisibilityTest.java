package br.com.muxe.core.common.visibility.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VisibilityTest {

    @Test
    void theSelectorCyclesThroughTheThreeOptions() {
        assertThat(Visibility.ALL.next()).isEqualTo(Visibility.FRIENDS);
        assertThat(Visibility.FRIENDS.next()).isEqualTo(Visibility.NONE);
        assertThat(Visibility.NONE.next()).isEqualTo(Visibility.ALL);
    }

    @Test
    void eachOptionDecidesWhoIsShown() {
        assertThat(Visibility.ALL.shows(false)).isTrue();
        assertThat(Visibility.FRIENDS.shows(true)).isTrue();
        assertThat(Visibility.FRIENDS.shows(false)).isFalse();
        assertThat(Visibility.NONE.shows(true)).isFalse();
    }

    @Test
    void unknownStoredValuesFallBackToShowingEveryone() {
        assertThat(Visibility.parse("FRIENDS")).isEqualTo(Visibility.FRIENDS);
        assertThat(Visibility.parse("something")).isEqualTo(Visibility.ALL);
        assertThat(Visibility.parse(null)).isEqualTo(Visibility.ALL);
    }
}
