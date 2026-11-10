package br.com.muxe.minigame.api.view;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class EndTitlesTest {

    @Test
    void theWordsComeFromEachLanguageOfTheConfig() {
        YamlConfiguration portuguese = new YamlConfiguration();
        portuguese.set("end-titles.victory.word", "VITÓRIA");
        portuguese.set("end-titles.victory.accent", "&6");
        portuguese.set("end-titles.defeat.word", "DERROTA");
        YamlConfiguration english = new YamlConfiguration();
        english.set("end-titles.victory.word", "VICTORY!");
        Localized<Optional<ConfigurationSection>> sections = LocalizedConfig.of(portuguese, Map.of(Language.ENGLISH, english))
            .map(root -> Optional.ofNullable(root.getConfigurationSection("end-titles")));

        EndTitles titles = EndTitles.from(null, sections, LanguageLookup.DEFAULT);

        assertThat(titles.sweep(true, Language.PORTUGUESE).word()).isEqualTo("VITÓRIA");
        assertThat(titles.sweep(true, Language.ENGLISH).word()).isEqualTo("VICTORY!");
        assertThat(titles.sweep(true, Language.ENGLISH).accent()).isEqualTo("&6");
        assertThat(titles.sweep(false, Language.ENGLISH).word()).isEqualTo("DERROTA");
    }

    @Test
    void withoutConfigTheWordsHaveAPortugueseAndAnEnglishDefault() {
        EndTitles titles = EndTitles.from(null, Localized.same(Optional.empty()), LanguageLookup.DEFAULT);

        assertThat(titles.sweep(true, Language.PORTUGUESE).word()).isEqualTo("VITÓRIA");
        assertThat(titles.sweep(true, Language.ENGLISH).word()).isEqualTo("VICTORY");
        assertThat(titles.sweep(false, Language.ENGLISH).word()).isEqualTo("DEFEAT");
    }
}
