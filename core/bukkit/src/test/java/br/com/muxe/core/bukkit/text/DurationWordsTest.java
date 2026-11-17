package br.com.muxe.core.bukkit.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationWordsTest {

    private static final DurationWords WORDS = DurationWords.PORTUGUESE;

    @Test
    void theFullTimeSpellsEveryUnitThatIsNotZero() {
        Duration left = Duration.ofDays(21).plusHours(21).plusMinutes(54).plusSeconds(3);

        assertThat(WORDS.full(left)).isEqualTo("21 dias 21 horas 54 minutos 3 segundos");
        assertThat(WORDS.full(Duration.ofDays(1).plusMinutes(1))).isEqualTo("1 dia 1 minuto");
        assertThat(WORDS.full(Duration.ZERO)).isEqualTo("0 segundos");
    }

    @Test
    void theLargestUnitRoundsDown() {
        assertThat(WORDS.largest(Duration.ofDays(30))).isEqualTo("30 dias");
        assertThat(WORDS.largest(Duration.ofDays(13).plusHours(4))).isEqualTo("13 dias");
        assertThat(WORDS.largest(Duration.ofHours(1).plusMinutes(59))).isEqualTo("1 hora");
        assertThat(WORDS.largest(Duration.ofSeconds(1))).isEqualTo("1 segundo");
    }
}
