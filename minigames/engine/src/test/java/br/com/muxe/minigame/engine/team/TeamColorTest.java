package br.com.muxe.minigame.engine.team;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.domain.Language;
import org.junit.jupiter.api.Test;

class TeamColorTest {

    @Test
    void everyTeamHasAPortugueseAndAnEnglishName() {
        assertThat(TeamColor.VERMELHO.label().in(Language.PORTUGUESE)).isEqualTo("Vermelho");
        assertThat(TeamColor.VERMELHO.label().in(Language.ENGLISH)).isEqualTo("Red");
        assertThat(TeamColor.CIANO.displayName(Language.ENGLISH)).isEqualTo("Aqua");
        for (TeamColor color : TeamColor.values()) {
            assertThat(color.displayName(Language.ENGLISH)).isNotBlank().isNotEqualTo(color.displayName());
        }
    }

    @Test
    void theLetterStaysTheSameInEveryLanguage() {
        assertThat(TeamColor.AZUL.letter()).isEqualTo('A');
    }
}
