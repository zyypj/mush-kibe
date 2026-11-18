package br.com.muxe.core.bukkit.preference;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.preference.PreferencesPages.Click;
import br.com.muxe.core.bukkit.preference.PreferencesPages.Shown;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.preference.domain.Preference;
import br.com.muxe.core.common.preference.domain.Preferences;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PreferencesPagesTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(PreferencesPagesTest.class, "config.yml");
    private static final PreferencesSettings PORTUGUESE = PreferencesSettings.from(TEXTS.in(Language.PORTUGUESE).getConfigurationSection("menus.preferences"));
    private static final PreferencesSettings ENGLISH = PreferencesSettings.from(TEXTS.in(Language.ENGLISH).getConfigurationSection("menus.preferences"));

    @Test
    void theGeneralTabIsTheReferenceOneWithItsPaneLitAndAWayBackToTheProfile() {
        List<Shown> page = PreferencesPages.page(PORTUGUESE, "geral", Preferences.DEFAULTS, true);

        assertThat(PORTUGUESE.title()).isEqualTo("Preferências");
        assertThat(at(page, 1).icon().lore()).containsExactly(" ", "§aSelecionado.");
        assertThat(at(page, 2).icon().lore()).containsExactly(" ", "§eClique para selecionar.");
        assertThat(at(page, 3).icon().material()).isEqualTo(Material.HOPPER);
        assertThat(at(page, 10).icon().data()).isEqualTo(PreferencesPages.PANE_LIME);
        assertThat(at(page, 11).icon().data()).isEqualTo(PreferencesPages.PANE_GRAY);
        assertThat(at(page, 29).icon().name()).isEqualTo("§aBate-papo");
        assertThat(at(page, 29).icon().lore()).containsExactly("§7Mostrar o bate-papo geral.", " ", "§7Valor: §aAtivado", " ", "§eClique para desativar.");
        assertThat(at(page, 38).icon().material()).isEqualTo(Material.INK_SACK);
        assertThat(at(page, 38).icon().data()).isEqualTo(PreferencesPages.DYE_ON);
        assertThat(at(page, 31).icon().name()).isEqualTo("§7Confirmar voltar para o lobby");
        assertThat(at(page, 31).icon().lore()).containsExactly(" ", "§7Valor: §cDesativado", " ", "§eClique para ativar.");
        assertThat(at(page, 40).icon().data()).isEqualTo(PreferencesPages.DYE_OFF);
        assertThat(at(page, 33).click()).isEqualTo(Click.OPEN);
        assertThat(at(page, 33).target()).contains(PreferencesMenu.TAG_FORMAT);
        assertThat(at(page, 42).click()).isEqualTo(Click.OPEN);
        assertThat(at(page, 42).icon().material()).isEqualTo(Material.STONE_BUTTON);
        assertThat(at(page, 42).icon().lore()).contains("§7Exclusivo para §d§lULTRA§6+");
        assertThat(at(page, 49).click()).isEqualTo(Click.BACK);
        assertThat(at(page, 49).icon().lore()).containsExactly("§7Para Meu perfil");
        assertThat(PreferencesPages.page(PORTUGUESE, "geral", Preferences.DEFAULTS, false)).noneMatch(shown -> shown.slot() == 49);
    }

    @Test
    void theSocialTabCyclesPrivateMessagesAndTogglesTheInvites() {
        List<Shown> page = PreferencesPages.page(PORTUGUESE, "social", Preferences.DEFAULTS, true);

        assertThat(at(page, 11).icon().data()).isEqualTo(PreferencesPages.PANE_LIME);
        assertThat(at(page, 28).icon().name()).isEqualTo("§aMensagens privadas");
        assertThat(at(page, 28).icon().lore()).containsExactly("§7Define quem pode te enviar", "§7mensagens privadas.", " ", "§a- §aPúblico§7 (Selecionado)",
            "§7- §eLimitado", "§7- §bAmigos", "§7- §cDesativado", " ", "§eClique para trocar para §eLimitado.");
        assertThat(at(page, 37).icon().data()).isEqualTo((short) 2);
        assertThat(at(page, 28).preference()).contains(Preference.PRIVATE_MESSAGES);
        assertThat(at(page, 30).icon().lore()).containsExactly("§7Permite receber convites", "§7de party.", " ", "§7Valor: §aAtivado", " ", "§eClique para desativar.");
        assertThat(at(page, 32).icon().material()).isEqualTo(Material.ITEM_FRAME);
        assertThat(at(page, 34).preference()).contains(Preference.FRIEND_REQUESTS);
        assertThat(at(page, 43).click()).isEqualTo(Click.CHANGE);
    }

    @Test
    void thePrivacyTabAndAChangedPreferenceShowTheirValue() {
        Preferences hidden = Preferences.DEFAULTS.with(Preference.STATS_VISIBLE, "false").with(Preference.PRIVATE_MESSAGES, "OFF");

        List<Shown> page = PreferencesPages.page(PORTUGUESE, "privacidade", hidden, true);

        assertThat(at(page, 12).icon().data()).isEqualTo(PreferencesPages.PANE_LIME);
        assertThat(at(page, 29).icon().name()).isEqualTo("§7Visibilidade de estatísticas");
        assertThat(at(page, 29).icon().lore()).containsExactly("§7Permite outros jogadores", "§7visualizarem suas estatísticas", "§7utilizando o /stats", " ",
            "§7Valor: §cDesativado", " ", "§eClique para ativar.");
        assertThat(at(page, 31).icon().material()).isEqualTo(Material.LEASH);
        assertThat(at(page, 33).icon().material()).isEqualTo(Material.ICE);
        assertThat(at(PreferencesPages.page(PORTUGUESE, "social", hidden, true), 28).icon().lore()).endsWith("§eClique para trocar para §aPúblico.");
    }

    @Test
    void theEnglishMenuIsTranslated() {
        List<Shown> page = PreferencesPages.page(ENGLISH, "social", Preferences.DEFAULTS, true);

        assertThat(ENGLISH.title()).isEqualTo("Preferences");
        assertThat(at(page, 28).icon().lore()).contains("§a- §aPublic§7 (Selected)", "§eClick to change to §eLimited.");
        assertThat(at(page, 30).icon().lore()).contains("§7Value: §aOn", "§eClick to turn off.");
    }

    @Test
    void lobbyAsksOnceAndGoesOnlyWhenAskedAgainWithinFiveSeconds() {
        LobbyConfirmations confirmations = new LobbyConfirmations();
        UUID player = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-15T12:00:00Z");

        assertThat(confirmations.confirmed(player, now)).isFalse();
        assertThat(confirmations.confirmed(player, now.plusSeconds(3))).isTrue();
        assertThat(confirmations.confirmed(player, now.plusSeconds(4))).isFalse();
        assertThat(confirmations.confirmed(player, now.plusSeconds(10))).isFalse();
        assertThat(confirmations.confirmed(player, now.plusSeconds(15))).isTrue();
    }

    private static Shown at(List<Shown> page, int slot) {
        return page.stream().filter(shown -> shown.slot() == slot).findFirst().orElseThrow(() -> new AssertionError("Nothing at slot " + slot));
    }
}
