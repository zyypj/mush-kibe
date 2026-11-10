package br.com.muxe.lobby.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.scoreboard.SidebarContent;
import br.com.muxe.core.common.config.live.LiveValue;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.lobby.MuxeLobbyPlugin;
import br.com.muxe.lobby.config.LobbySettings;
import br.com.muxe.lobby.profile.PlayerInfo;
import br.com.muxe.lobby.profile.RankInfoSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LobbyBoardTest {

    private static final Localized<LobbySettings> SETTINGS = LocalizedConfig.bundled(MuxeLobbyPlugin.class, "config.yml").map(LobbySettings::from);

    private final UUID reader = UUID.randomUUID();
    private final Map<UUID, Language> languages = new HashMap<>();
    private final LiveValue<Localized<ScoreboardTemplate>> templates = new LiveValue<>(SETTINGS.map(LobbySettings::scoreboard));
    private final RankInfoSource info = new RankInfoSource(player -> Optional.empty(), player -> 1_500L, SETTINGS.map(LobbySettings::defaultInfo),
        player -> languages.getOrDefault(player, Language.DEFAULT));
    private final LobbyBoard board = new LobbyBoard(templates, player -> languages.getOrDefault(player, Language.DEFAULT), info);

    @Test
    void aPortugueseReaderSeesThePortugueseScoreboard() {
        SidebarContent content = board.content(reader, "4", 2211);

        assertThat(content.title()).isEqualTo("&b&lMUXE");
        assertThat(content.lines()).containsExactly(
            "",
            "&fTag: &7Membro",
            "&fCréditos: &d1500",
            "",
            "&fLobby: &a#4",
            "&fJogadores: &b2211",
            "",
            "&amuxe.com.br"
        );
    }

    @Test
    void anEnglishReaderSeesTheEnglishScoreboard() {
        languages.put(reader, Language.ENGLISH);

        SidebarContent content = board.content(reader, "4", 2211);

        assertThat(content.title()).isEqualTo("&b&lMUXE");
        assertThat(content.lines()).containsExactly(
            "",
            "&fTag: &7Member",
            "&fCredits: &d1500",
            "",
            "&fLobby: &a#4",
            "&fPlayers: &b2211",
            "",
            "&amuxe.com.br"
        );
    }

    @Test
    void theNextRenderFollowsALanguageSwitch() {
        assertThat(board.content(reader, "1", 1).lines()).contains("&fJogadores: &b1");

        languages.put(reader, Language.ENGLISH);

        assertThat(board.content(reader, "1", 1).lines()).contains("&fPlayers: &b1");
    }

    @Test
    void aLiveReloadIsSeenInEveryLanguage() {
        UUID portuguese = UUID.randomUUID();
        languages.put(reader, Language.ENGLISH);
        templates.swap(Localized.of(language -> new ScoreboardTemplate("&aT", List.of(language == Language.ENGLISH ? "&fPlayers: {players}" : "&fJogadores: {players}"))));

        assertThat(board.content(reader, "1", 7).lines()).containsExactly("&fPlayers: 7");
        assertThat(board.content(portuguese, "1", 7).lines()).containsExactly("&fJogadores: 7");
    }

    @Test
    void theDefaultTagIsReadFromTheSettingsOfEachLanguage() {
        assertThat(SETTINGS.in(Language.ENGLISH).defaultInfo()).isEqualTo(new PlayerInfo("&7Member", 0));
        assertThat(SETTINGS.in(Language.PORTUGUESE).defaultInfo()).isEqualTo(new PlayerInfo("&7Membro", 0));
    }
}
