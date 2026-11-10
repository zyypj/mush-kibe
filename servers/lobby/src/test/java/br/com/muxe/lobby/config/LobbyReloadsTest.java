package br.com.muxe.lobby.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.config.edit.ConfigReloads;
import br.com.muxe.core.bukkit.config.edit.ReloadReport;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.config.live.LiveValue;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.lobby.scoreboard.ScoreboardTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LobbyReloadsTest {

    private final List<String> spawnReloads = new ArrayList<>();
    private final LiveValue<Localized<ScoreboardTemplate>> scoreboard = new LiveValue<>(Localized.same(new ScoreboardTemplate("old", List.of("a"))));
    private final YamlConfiguration english = new YamlConfiguration();
    private final ConfigReloads reloads = LobbyReloads.create(scoreboard, edited -> LocalizedConfig.of((YamlConfiguration) edited, Map.of(Language.ENGLISH, english)),
        () -> spawnReloads.add("spawn"));
    private final YamlConfiguration config = new YamlConfiguration();

    @Test
    void theSpawnReloadsOnlyWhenItsPathChanges() {
        reloads.apply(List.of("spawn.x"), config);
        reloads.apply(List.of("arrival.fly-height"), config);

        assertThat(spawnReloads).containsExactly("spawn");
    }

    @Test
    void theScoreboardTemplateIsReplacedFromTheConfigurationOnTitleOrLineChanges() {
        config.set("scoreboard.title", "&aNew");
        config.set("scoreboard.lines", List.of("x", "y"));

        ReloadReport report = reloads.apply(List.of("scoreboard.lines"), config);

        assertThat(report.live()).isTrue();
        assertThat(scoreboard.get().in(Language.PORTUGUESE)).isEqualTo(new ScoreboardTemplate("&aNew", List.of("x", "y")));
    }

    @Test
    void theScoreboardReloadsForEveryLanguageAndEnglishFallsBackToThePortugueseTitle() {
        config.set("scoreboard.title", "&aNovo");
        config.set("scoreboard.lines", List.of("&fJogadores"));
        english.set("scoreboard.lines", List.of("&fPlayers"));

        reloads.apply(List.of("scoreboard.title"), config);

        assertThat(scoreboard.get().in(Language.PORTUGUESE)).isEqualTo(new ScoreboardTemplate("&aNovo", List.of("&fJogadores")));
        assertThat(scoreboard.get().in(Language.ENGLISH)).isEqualTo(new ScoreboardTemplate("&aNovo", List.of("&fPlayers")));
    }

    @Test
    void otherScoreboardSettingsAreNotClaimedAsLive() {
        assertThat(reloads.covers("scoreboard.default-tag")).isFalse();
        assertThat(reloads.covers("scoreboard.update-interval")).isFalse();
        assertThat(reloads.covers("launch-pad.enabled")).isFalse();
    }
}
