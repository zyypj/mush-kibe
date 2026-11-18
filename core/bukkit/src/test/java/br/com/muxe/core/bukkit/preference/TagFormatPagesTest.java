package br.com.muxe.core.bukkit.preference;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.preference.TagFormatPages.Click;
import br.com.muxe.core.bukkit.preference.TagFormatPages.Shown;
import br.com.muxe.core.bukkit.preference.TagFormatPages.Viewer;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.preference.domain.TagFormat;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class TagFormatPagesTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(TagFormatPagesTest.class, "config.yml");
    private static final TagFormatSettings PORTUGUESE = TagFormatSettings.from(TEXTS.in(Language.PORTUGUESE).getConfigurationSection("menus.tag-format"));
    private static final TagFormatSettings ENGLISH = TagFormatSettings.from(TEXTS.in(Language.ENGLISH).getConfigurationSection("menus.tag-format"));
    private static final RankDisplay ULTRA_PLUS = new RankDisplay(Optional.of(new Tag("ultra+", "Ultra&6+", "&5", 50, Optional.of("&5&lULTRA&6+ "), false)));

    @Test
    void anUltraPlusPlayerSeesEveryFormatWithAnExampleOfTheirOwnTag() {
        List<Shown> page = TagFormatPages.page(PORTUGUESE, new Viewer("tadeu", ULTRA_PLUS, true, TagFormat.DEFAULT), true);

        assertThat(PORTUGUESE.title()).isEqualTo("Formato da tag");
        assertThat(PORTUGUESE.rows()).isEqualTo(3);
        assertThat(at(page, 10).icon().material()).isEqualTo(Material.INK_SACK);
        assertThat(at(page, 10).icon().data()).isEqualTo((short) 15);
        assertThat(at(page, 10).icon().name()).isEqualTo("§aPadrão");
        assertThat(at(page, 10).icon().lore()).containsExactly(" ", "§7Exemplo:", "§5§lULTRA§6+ §5tadeu", " ", "§aSelecionado.");
        assertThat(at(page, 10).icon().isGlowing()).isTrue();
        assertThat(at(page, 11).icon().name()).isEqualTo("§aCor");
        assertThat(at(page, 11).icon().lore()).containsExactly(" ", "§7Exemplo:", "§5tadeu§6+", " ", "§7Exclusivo para §d§lULTRA§6+", " ", "§eClique para selecionar.");
        assertThat(at(page, 12).icon().lore().get(2)).isEqualTo("§5[ULTRA§6+§5] §5tadeu");
        assertThat(at(page, 13).icon().lore().get(2)).isEqualTo("§5[Ultra§6+§5] §5tadeu");
        assertThat(at(page, 14).icon().lore().get(2)).isEqualTo("§5§lULTRA§6+ §ftadeu");
        assertThat(at(page, 15).icon().name()).isEqualTo("§aPadrão (Cinza)");
        assertThat(at(page, 15).icon().lore().get(2)).isEqualTo("§5§lULTRA§6+ §7tadeu");
        assertThat(at(page, 15).click()).isEqualTo(Click.SELECT);
        assertThat(at(page, 15).format()).contains(TagFormat.DEFAULT_GRAY);
        assertThat(at(page, 21).click()).isEqualTo(Click.BACK);
        assertThat(at(page, 21).icon().lore()).containsExactly("§7Para Preferências");
    }

    @Test
    void theChosenFormatIsSelectedAndAPlayerWithoutUltraPlusOnlyHasTheDefault() {
        List<Shown> chosen = TagFormatPages.page(PORTUGUESE, new Viewer("tadeu", ULTRA_PLUS, true, TagFormat.BRACKETS), false);
        List<Shown> locked = TagFormatPages.page(PORTUGUESE, new Viewer("tadeu", ULTRA_PLUS, false, TagFormat.BRACKETS), false);

        assertThat(at(chosen, 13).icon().lore()).endsWith("§aSelecionado.");
        assertThat(at(chosen, 13).icon().isGlowing()).isTrue();
        assertThat(at(chosen, 10).icon().lore()).endsWith("§eClique para selecionar.");
        assertThat(chosen).noneMatch(shown -> shown.slot() == 21);
        assertThat(at(locked, 10).icon().lore()).endsWith("§aSelecionado.");
        assertThat(at(locked, 13).icon().lore()).endsWith("§cBloqueado.");
        assertThat(at(locked, 13).click()).isEqualTo(Click.LOCKED);
        assertThat(at(locked, 10).click()).isEqualTo(Click.SELECT);
    }

    @Test
    void theEnglishMenuIsTranslated() {
        List<Shown> page = TagFormatPages.page(ENGLISH, new Viewer("tadeu", ULTRA_PLUS, true, TagFormat.DEFAULT), true);

        assertThat(ENGLISH.title()).isEqualTo("Tag format");
        assertThat(at(page, 12).icon().name()).isEqualTo("§aBrackets (Uppercase)");
        assertThat(at(page, 11).icon().lore()).contains("§7Example:", "§7Exclusive to §d§lULTRA§6+", "§eClick to select.");
        assertThat(at(page, 21).icon().lore()).containsExactly("§7To Preferences");
    }

    private static Shown at(List<Shown> page, int slot) {
        return page.stream().filter(shown -> shown.slot() == slot).findFirst().orElseThrow(() -> new AssertionError("Nothing at slot " + slot));
    }
}
