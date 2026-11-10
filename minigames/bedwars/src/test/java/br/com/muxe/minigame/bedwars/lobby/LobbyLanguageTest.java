package br.com.muxe.minigame.bedwars.lobby;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.scoreboard.SidebarContent;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.bedwars.BedWarsPlugin;
import br.com.muxe.minigame.bedwars.lobby.format.LevelBadges;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyNpc;
import br.com.muxe.minigame.bedwars.lobby.settings.LobbyScoreboardSettings;
import br.com.muxe.minigame.bedwars.lobby.settings.StatsNpcSettings;
import br.com.muxe.minigame.bedwars.lobby.top.TopTexts;
import br.com.muxe.minigame.bedwars.stats.domain.PlayerStats;
import br.com.muxe.minigame.bedwars.stats.domain.StatsPeriod;
import br.com.muxe.minigame.engine.mode.ModeNames;
import java.util.List;
import org.junit.jupiter.api.Test;

class LobbyLanguageTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(BedWarsPlugin.class, "config.yml");
    private static final LevelBadges BADGES = LevelBadges.from(TEXTS.standard().getConfigurationSection("levels"));
    private static final Localized<LobbyScoreboardSettings> SCOREBOARDS = TEXTS.map(root -> LobbyScoreboardSettings.from(root.getConfigurationSection("lobby.scoreboard")));
    private static final ModeNames MODES = ModeNames.from(TEXTS);
    private static final List<String> SHOWN = List.of("solo", "1v1");

    @Test
    void theLobbySidebarIsWrittenInTheLanguageOfItsViewer() {
        SidebarContent portuguese = sidebar(Language.PORTUGUESE);
        SidebarContent english = sidebar(Language.ENGLISH);

        assertThat(portuguese.lines()).contains("Seu nível: &7[1&l✫&7]", " Vitórias: &a0", "Jogadores: &b652");
        assertThat(english.lines()).contains("Your level: &7[1&l✫&7]", " Wins: &a0", "Players: &b652");
        assertThat(english.lines()).hasSameSizeAs(portuguese.lines());
        assertThat(english.title()).isEqualTo(portuguese.title());
    }

    @Test
    void theModesOfTheSidebarAreNamedInTheLanguageOfItsViewer() {
        assertThat(MODES.in(Language.PORTUGUESE, "duplas")).isEqualTo("Duplas");
        assertThat(MODES.in(Language.ENGLISH, "duplas")).isEqualTo("Doubles");
        assertThat(sidebar(Language.ENGLISH).lines()).contains("&eSolo:");
    }

    @Test
    void theLobbyNpcsAndTheStatsNpcAreLabelledPerViewer() {
        Localized<List<LobbyNpc>> npcs = TEXTS.map(root -> LobbyNpc.from(root.getConfigurationSection("lobby.npcs")));
        Localized<StatsNpcSettings> stats = TEXTS.map(root -> StatsNpcSettings.from(root.getConfigurationSection("lobby.stats-npc")));

        assertThat(npcs.in(Language.ENGLISH)).extracting(LobbyNpc::id).isEqualTo(npcs.standard().stream().map(LobbyNpc::id).toList());
        assertThat(npcs.in(Language.ENGLISH)).extracting(LobbyNpc::action).isEqualTo(npcs.standard().stream().map(LobbyNpc::action).toList());
        assertThat(npcs.in(Language.ENGLISH).getFirst().lines()).isNotEqualTo(npcs.standard().getFirst().lines());
        assertThat(stats.in(Language.ENGLISH).lines().getFirst()).startsWith("Your level");
        assertThat(stats.in(Language.ENGLISH).barSegments()).isEqualTo(stats.standard().barSegments());
    }

    @Test
    void theTopBoardsNameThePeriodsInTheLanguageOfTheirViewer() {
        TopTexts portuguese = TopTexts.from(TEXTS.in(Language.PORTUGUESE).getConfigurationSection("lobby.tops"));
        TopTexts english = TopTexts.from(TEXTS.in(Language.ENGLISH).getConfigurationSection("lobby.tops"));

        assertThat(english.periodNames().get(StatsPeriod.DAILY)).isNotEqualTo(portuguese.periodNames().get(StatsPeriod.DAILY));
        assertThat(english.refreshSeconds()).isEqualTo(portuguese.refreshSeconds());
        assertThat(english.spacing()).isEqualTo(portuguese.spacing());
    }

    private static SidebarContent sidebar(Language language) {
        return LobbySidebar.content(SCOREBOARDS.in(language), PlayerStats.EMPTY, BADGES, SHOWN, mode -> MODES.in(language, mode), 652);
    }
}
