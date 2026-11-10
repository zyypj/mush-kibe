package br.com.muxe.minigame.api.view;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.minigame.engine.team.TeamColor;
import org.junit.jupiter.api.Test;

class NameTagStyleTest {

    @Test
    void theTagShowsTheTeamLetterInBold() {
        NameTag tag = NameTagStyle.of("mxbw", TeamColor.VERMELHO);

        assertThat(tag.prefix()).isEqualTo("§c§lV§c ");
    }

    @Test
    void aRenamedTeamShowsTheFirstLetterOfItsNewName() {
        NameTag tag = NameTagStyle.of("mxbw", TeamColor.VERMELHO, 'T');

        assertThat(tag.prefix()).isEqualTo("§c§lT§c ");
        assertThat(tag.team()).isEqualTo(NameTagStyle.of("mxbw", TeamColor.VERMELHO).team());
    }
}
