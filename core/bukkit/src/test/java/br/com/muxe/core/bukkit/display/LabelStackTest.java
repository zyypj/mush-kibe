package br.com.muxe.core.bukkit.display;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class LabelStackTest {

    @Test
    void downwardKeepsTheFirstLineInPlace() {
        List<DisplayLabel> labels = LabelStack.downward(List.of("a", "b", "c"), 2.0, 0.25);

        assertThat(labels).extracting(DisplayLabel::height).containsExactly(2.0, 1.75, 1.5);
        assertThat(labels).extracting(DisplayLabel::text).containsExactly("a", "b", "c");
    }

    @Test
    void upwardKeepsTheLastLineInPlaceAndUsesTheGapsFromTheBottom() {
        List<DisplayLabel> labels = LabelStack.upward(List.of("top", "middle", "bottom"), 1.0, List.of(0.5), 0.25);

        assertThat(labels.get(2).height()).isEqualTo(1.0);
        assertThat(labels.get(1).height()).isCloseTo(1.5, within(1e-9));
        assertThat(labels.get(0).height()).isCloseTo(1.75, within(1e-9));
    }

    @Test
    void anEmptyListHasNoLabelsAndTheSpacingMustBePositive() {
        assertThat(LabelStack.upward(List.of(), 1.0, List.of(), 0.25)).isEmpty();
        assertThatThrownBy(() -> LabelStack.downward(List.of("a"), 1.0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
