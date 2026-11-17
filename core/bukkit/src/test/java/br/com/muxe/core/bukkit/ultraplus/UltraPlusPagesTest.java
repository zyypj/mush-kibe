package br.com.muxe.core.bukkit.ultraplus;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.ultraplus.UltraPlusPages.Click;
import br.com.muxe.core.bukkit.ultraplus.UltraPlusPages.Shown;
import br.com.muxe.core.bukkit.ultraplus.UltraPlusPages.Viewer;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class UltraPlusPagesTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(UltraPlusPagesTest.class, "config.yml");
    private static final UltraPlusSettings PORTUGUESE = UltraPlusSettings.from(TEXTS.in(Language.PORTUGUESE));
    private static final UltraPlusSettings ENGLISH = UltraPlusSettings.from(TEXTS.in(Language.ENGLISH));
    private static final Instant EXPORTED_AT = Instant.parse("2026-11-17T21:42:41Z");
    private static final Viewer ONE_MONTH = new Viewer("tadeu", true, 1, UltraPlusStyle.DEFAULT);

    @Test
    void theUltraPlusMenuShowsTheClaimedRewardTheFirstTagAndTheProgressToTheNextOnes() {
        List<Shown> page = UltraPlusPages.main(PORTUGUESE, ONE_MONTH, true, EXPORTED_AT);

        assertThat(PORTUGUESE.main().title()).isEqualTo("Menu do Ultra+");
        assertThat(at(page, 12).icon().material()).isEqualTo(Material.MINECART);
        assertThat(at(page, 12).icon().lore()).containsExactly("§7Resgate a recompensa mensal", "§7do Ultra+!", " ", "§7Recompensa:", "§7- §d450 créditos", " ",
            "§bReinicia em 13 dias.", " ", "§cJá resgatado.");
        assertThat(at(page, 14).click()).isEqualTo(Click.COLORS);
        assertThat(at(page, 14).icon().name()).isEqualTo("§aCor do §6+ §a(/cormais)");
        assertThat(at(page, 28).icon().name()).isEqualTo("§dTag Nível I");
        assertThat(at(page, 28).icon().isGlowing()).isTrue();
        assertThat(at(page, 28).icon().lore()).containsExactly(" ", "§7Incluso no Ultra+", " ", "§7Exemplo:", "§5§lULTRA§6+ §5tadeu", " ", "§aSelecionada.");
        assertThat(at(page, 29).icon().name()).isEqualTo("§a-§7--------->");
        assertThat(at(page, 29).icon().lore()).containsExactly(" ", "§7Meses necessários:", "§a1/6 §7(16%)");
        assertThat(at(page, 30).icon().lore()).containsExactly(" ", "§7Meses necessários:", "§a1/6 §7(16%)", " ", "§7Exemplo:", "§b§lULTRA§6+ §btadeu", " ",
            "§cBloqueado.");
        assertThat(at(page, 31).icon().name()).isEqualTo("§7---------->");
        assertThat(at(page, 31).icon().lore()).containsExactly(" ", "§7Meses necessários:", "§a1/12 §7(8%)");
        assertThat(at(page, 33).icon().lore()).containsExactly(" ", "§7Meses necessários:", "§a1/24 §7(4%)");
        assertThat(at(page, 34).icon().name()).isEqualTo("§cTag Nível IV");
        assertThat(at(page, 39).click()).isEqualTo(Click.BACK);
        assertThat(at(page, 39).icon().lore()).containsExactly("§7Para Meu perfil");
    }

    @Test
    void anUnclaimedRewardInvitesTheClickAndALifetimeMemberHasEveryLevelUnlocked() {
        Viewer lifetime = new Viewer("tadeu", true, UltraPlusService.LIFETIME_MONTHS, UltraPlusStyle.DEFAULT.withTagLevel(4));

        List<Shown> page = UltraPlusPages.main(PORTUGUESE, lifetime, false, EXPORTED_AT);

        assertThat(at(page, 12).icon().lore()).endsWith("§eClique para resgatar.");
        assertThat(at(page, 28).icon().lore()).endsWith("§eClique para selecionar.");
        assertThat(at(page, 33).icon().name()).isEqualTo("§a----------§7>");
        assertThat(at(page, 33).icon().lore()).contains("§a24/24 §7(100%)");
        assertThat(at(page, 34).icon().lore()).contains("§c§lULTRA§6+ §ctadeu").endsWith("§aSelecionada.");
    }

    @Test
    void thePlusColorsUnlockOneByOneEachMonth() {
        List<Shown> page = UltraPlusPages.colors(PORTUGUESE, ONE_MONTH);

        assertThat(PORTUGUESE.colors().title()).isEqualTo("Cor do +");
        assertThat(at(page, 10).icon().name()).isEqualTo("§6+ §7(Dourado)");
        assertThat(at(page, 10).icon().lore()).containsExactly(" ", "§7Incluso no Ultra+", " ", "§7Exemplo:", "§5§lULTRA§6+ §5tadeu", " ", "§aSelecionada.");
        assertThat(at(page, 11).icon().lore()).containsExactly(" ", "§7Meses necessários:", "§a1/2 §7(50%)", " ", "§7Exemplo:", "§5§lULTRA§e+ §5tadeu", " ",
            "§cBloqueado.");
        assertThat(at(page, 13).icon().material()).isEqualTo(Material.NETHER_STALK);
        assertThat(at(page, 21).icon().material()).isEqualTo(Material.PRISMARINE_SHARD);
        assertThat(at(page, 23).icon().lore()).contains("§a1/12 §7(8%)");
        assertThat(at(page, 30).icon().lore()).containsExactly("§7Para Menu do Ultra+");
    }

    @Test
    void theEmblemsShowTheNickWithEachSymbolAndOnlyMembersCanPickThem() {
        List<Shown> page = UltraPlusPages.emblems(PORTUGUESE, ONE_MONTH);

        assertThat(PORTUGUESE.emblems().title()).isEqualTo("Emblemas");
        assertThat(at(page, 10).icon().name()).isEqualTo("§aNenhum ");
        assertThat(at(page, 10).icon().material()).isEqualTo(Material.BARRIER);
        assertThat(at(page, 11).icon().name()).isEqualTo("§aCoração ❤");
        assertThat(at(page, 11).icon().lore()).containsExactly(" ", "§7Exemplo:", "§5§lULTRA§6+ §5tadeu❤", " ", "§7Exclusivo para §d§lULTRA§6+", " ",
            "§eClique para selecionar.");
        assertThat(at(page, 22).icon().lore()).contains("§5§lULTRA§6+ §5tadeu§6+");
        assertThat(at(page, 22).value()).contains("mais");
        assertThat(at(page, 30).icon().lore()).containsExactly("§7Para Meu perfil");

        Viewer chose = new Viewer("tadeu", true, 1, UltraPlusStyle.DEFAULT.withEmblem(Optional.of("flor")));
        assertThat(at(UltraPlusPages.emblems(PORTUGUESE, chose), 12).icon().lore()).endsWith("§aSelecionado.");
        assertThat(at(UltraPlusPages.emblems(PORTUGUESE, new Viewer("Steve", false, 0, UltraPlusStyle.DEFAULT)), 11).icon().lore()).endsWith("§cBloqueado.");
    }

    @Test
    void theMenusAreTranslated() {
        assertThat(ENGLISH.main().title()).isEqualTo("Ultra+ menu");
        assertThat(at(UltraPlusPages.main(ENGLISH, ONE_MONTH, false, EXPORTED_AT), 12).icon().lore()).contains("§bResets in 13 days.", "§eClick to claim.");
        assertThat(at(UltraPlusPages.emblems(ENGLISH, ONE_MONTH), 11).icon().name()).isEqualTo("§aHeart ❤");
    }

    private static Shown at(List<Shown> page, int slot) {
        return page.stream().filter(shown -> shown.slot() == slot).findFirst().orElseThrow(() -> new AssertionError("Nothing at slot " + slot));
    }
}
