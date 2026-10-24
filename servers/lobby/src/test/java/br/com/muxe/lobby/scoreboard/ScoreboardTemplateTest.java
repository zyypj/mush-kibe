package br.com.muxe.lobby.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.lobby.profile.PlayerInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreboardTemplateTest {

    private final ScoreboardTemplate template = new ScoreboardTemplate("&b&lMUXE", List.of(
        "",
        "&fTag: {tag}",
        "&fCréditos: &d{credits}",
        "",
        "&fLobby: &a#{lobby}",
        "&fJogadores: &b{players}",
        "",
        "&amuxe.com.br"
    ));

    @Test
    void fillsEveryPlaceholder() {
        List<String> lines = template.render(new PlayerInfo("&7Membro", 0), "4", 2211);

        assertThat(lines).containsExactly(
            "",
            "&fTag: &7Membro",
            "&fCréditos: &d0",
            "",
            "&fLobby: &a#4",
            "&fJogadores: &b2211",
            "",
            "&amuxe.com.br"
        );
        assertThat(template.title()).isEqualTo("&b&lMUXE");
    }

    @Test
    void thePlayerCountHasNoThousandsSeparator() {
        assertThat(template.render(new PlayerInfo("x", 1500), "1", 12345)).contains("&fJogadores: &b12345", "&fCréditos: &d1500");
    }
}
