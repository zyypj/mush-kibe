package br.com.muxe.minigame.api.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.timeline.Timeline.Entry;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineTest {

    @Test
    void theTimelineNamesTheNextEventAndWhenItHappens() {
        Timeline<String> timeline = new Timeline<>(List.of(
            new Entry<>("emerald-2", 720, "Esmeralda II"),
            new Entry<>("diamond-2", 360, "Diamante II")
        ));

        assertThat(timeline.next(109).orElseThrow().name()).isEqualTo("Diamante II");
        assertThat(timeline.next(109).orElseThrow().clock()).isEqualTo("4:11");
        assertThat(timeline.next(360).orElseThrow().name()).isEqualTo("Esmeralda II");
        assertThat(timeline.dueAt(720)).containsExactly("emerald-2");
        assertThat(timeline.next(720)).isEmpty();
    }

    @Test
    void anEmptyTimelineHasNothingDue() {
        assertThat(Timeline.empty().next(0)).isEmpty();
        assertThat(Timeline.empty().dueAt(0)).isEmpty();
    }

    @Test
    void anEventCannotHappenBeforeTheStart() {
        assertThatThrownBy(() -> new Entry<>("x", -1, "X")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEventNameCanBeTranslated() {
        Timeline<String> timeline = new Timeline<>(List.of(new Entry<>("diamond-2", 360, Text.of("Diamante II", "Diamond II"))));

        assertThat(timeline.next(0).orElseThrow().label().in(Language.ENGLISH)).isEqualTo("Diamond II");
        assertThat(timeline.next(0).orElseThrow().name()).isEqualTo("Diamante II");
    }
}
