package br.com.muxe.minigame.bedwars.room;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.cosmetic.catalog.CosmeticDefaults;
import br.com.muxe.minigame.api.cosmetic.message.KillAnnouncer;
import br.com.muxe.minigame.api.cosmetic.message.LifetimeTally;
import br.com.muxe.minigame.api.cosmetic.message.MessageSets;
import br.com.muxe.minigame.api.timeline.Timeline;
import br.com.muxe.minigame.bedwars.BedWarsPlugin;
import br.com.muxe.minigame.bedwars.game.BedWarsEvent;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class MatchLanguageTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(BedWarsPlugin.class, "config.yml");
    private static final MessageCatalog MESSAGES = MessageCatalog.from(TEXTS, "messages");
    private static final Localized<BedWarsSettings> SETTINGS = BedWarsSettings.from(TEXTS);

    @Test
    void theGeneratorUpgradeIsAnnouncedInEachReadersLanguage() {
        Text diamond = BedWarsMatch.generatorUpgrade(MESSAGES, Material.DIAMOND, 2);
        Text emerald = BedWarsMatch.generatorUpgrade(MESSAGES, Material.EMERALD, 3);

        assertThat(plain(diamond.in(Language.PORTUGUESE))).isEqualTo("Geradores de Diamante melhorados para nível II.");
        assertThat(plain(diamond.in(Language.ENGLISH))).isEqualTo("Diamond Generators have been upgraded to Tier II.");
        assertThat(plain(emerald.in(Language.ENGLISH))).isEqualTo("Emerald Generators have been upgraded to Tier III.");
    }

    @Test
    void aDestroyedBedIsAnnouncedWithTheTeamNameOfEachReader() {
        Localized<MessageSets> sets = Localized.of(language ->
            MessageSets.from(CosmeticDefaults.localized().in(language).killMessages(TEXTS.in(language).getConfigurationSection("kill-messages"))));
        KillAnnouncer announcer = new KillAnnouncer(sets, id -> sets.standard().find("padrao").orElseThrow(), new LifetimeTally());

        Text bed = announcer.bedText(UUID.randomUUID(), "§9Alice", "§c", TeamColor.VERMELHO.label());

        assertThat(plain(bed.in(Language.PORTUGUESE))).contains("Vermelho").contains("Alice").doesNotContain("Red");
        assertThat(plain(bed.in(Language.ENGLISH))).contains("Red Bed").contains("Alice").doesNotContain("Vermelho");
    }

    @Test
    void theEventsOfTheTimelineAreNamedInEachReadersLanguage() {
        Timeline<BedWarsEvent> timeline = SETTINGS.standard().timeline();
        Timeline.Entry<BedWarsEvent> diamond = timeline.entries().getFirst();

        assertThat(diamond.label().in(Language.PORTUGUESE)).isEqualTo("Diamante II");
        assertThat(diamond.label().in(Language.ENGLISH)).isEqualTo("Diamond II");
        assertThat(SETTINGS.in(Language.ENGLISH).timeline().entries()).isEqualTo(timeline.entries());
        assertThat(timeline.next(0).orElseThrow().label().in(Language.ENGLISH)).isEqualTo("Diamond II");
    }

    @Test
    void theShopKeepersAndTheGeneratorsAreLabelledInEachReadersLanguage() {
        assertThat(SETTINGS.in(Language.PORTUGUESE).holograms().shop()).containsExactly("{color}&lLOJA", "&7Clique para ver mais.");
        assertThat(SETTINGS.in(Language.ENGLISH).holograms().shop()).containsExactly("{color}&lSHOP", "&7Click to see more.");
        assertThat(SETTINGS.in(Language.ENGLISH).holograms().diamond()).contains("&b&lDiamond");
        assertThat(SETTINGS.in(Language.ENGLISH).holograms().diamond()).hasSameSizeAs(SETTINGS.standard().holograms().diamond());
    }

    @Test
    void theItemsGivenToAPlayerAreNamedInTheirLanguage() {
        assertThat(SETTINGS.in(Language.PORTUGUESE).items().of(Material.WOOD_SWORD)).contains("Espada de Madeira");
        assertThat(SETTINGS.in(Language.ENGLISH).items().of(Material.WOOD_SWORD)).contains("Wooden Sword");
    }

    @Test
    void theChestLabelAndTheSlingshotFollowTheReader() {
        assertThat(MESSAGES.lines(Language.ENGLISH, "chest-hologram")).hasSameSizeAs(MESSAGES.lines(Language.PORTUGUESE, "chest-hologram"));
        assertThat(MESSAGES.get(Language.PORTUGUESE, "slingshot-label")).isEqualTo("ESTILINGUE:");
        assertThat(MESSAGES.get(Language.ENGLISH, "slingshot-label")).isEqualTo("SLINGSHOT:");
        assertThat(MESSAGES.get(Language.ENGLISH, "slingshot-item")).isEqualTo("§cSlingshot");
        assertThat(MESSAGES.get(Language.ENGLISH, "decimal-separator")).isEqualTo(".");
        assertThat(MESSAGES.get(Language.PORTUGUESE, "decimal-separator")).isEqualTo(",");
        assertThat(List.of(MESSAGES.get(Language.PORTUGUESE, "beds-destroyed-event"), MESSAGES.get(Language.ENGLISH, "beds-destroyed-event"))).doesNotHaveDuplicates();
    }

    private static String plain(String text) {
        return ChatColor.stripColor(text);
    }
}
