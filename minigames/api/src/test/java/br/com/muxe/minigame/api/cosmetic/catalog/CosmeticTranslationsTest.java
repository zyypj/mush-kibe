package br.com.muxe.minigame.api.cosmetic.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.language.domain.Text;
import br.com.muxe.minigame.api.cosmetic.message.KillAnnouncer;
import br.com.muxe.minigame.api.cosmetic.message.LifetimeTally;
import br.com.muxe.minigame.api.cosmetic.message.LifetimeTally.Totals;
import br.com.muxe.minigame.api.cosmetic.message.MessageSets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

class CosmeticTranslationsTest {

    private static final Set<String> COLOR_CODES_ONLY = Set.of("kill-messages.preview.team-color");

    private final LocalizedConfig texts = CosmeticDefaults.texts();

    @Test
    void everyReadableTextOfTheCosmeticCatalogHasAnEnglishVersion() {
        Set<String> missing = new LinkedHashSet<>(texts.untranslated(Language.ENGLISH, ""));
        missing.removeAll(COLOR_CODES_ONLY);

        assertThat(missing).isEmpty();
    }

    @Test
    void theEnglishTextsKeepThePlaceholdersAndTheLinesOfThePortugueseOnes() {
        ConfigurationSection portuguese = texts.in(Language.PORTUGUESE);
        ConfigurationSection english = texts.in(Language.ENGLISH);
        for (Map.Entry<String, Object> entry : portuguese.getValues(true).entrySet()) {
            if (entry.getValue() instanceof String text) {
                assertThat(placeholders(english.getString(entry.getKey()))).as(entry.getKey()).isEqualTo(placeholders(text));
            } else if (entry.getValue() instanceof List<?> lines) {
                assertThat(english.getStringList(entry.getKey())).as(entry.getKey()).hasSameSizeAs(lines);
            }
        }
    }

    @Test
    void theCatalogNamesAndDescriptionsFollowTheLanguage() {
        Localized<CosmeticDefaults> defaults = CosmeticDefaults.localized();

        Cosmetic portuguese = defaults.in(Language.PORTUGUESE).catalog("bedwars", CosmeticKind.FINAL_KILL, null, id -> true).find("passarinhos").orElseThrow();
        Cosmetic english = defaults.in(Language.ENGLISH).catalog("bedwars", CosmeticKind.FINAL_KILL, null, id -> true).find("passarinhos").orElseThrow();

        assertThat(portuguese.name()).isEqualTo("Passarinhos");
        assertThat(english.name()).isEqualTo("Birds");
        assertThat(english.stars()).isEqualTo(portuguese.stars());
        assertThat(english.availability()).isEqualTo(portuguese.availability());
    }

    @Test
    void aKillIsToldInTheKillersSetAndInTheLanguageOfEachReader() {
        Localized<MessageSets> sets = CosmeticDefaults.localized().map(defaults -> MessageSets.from(defaults.killMessages(null)));
        LifetimeTally tally = new LifetimeTally();
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();
        tally.load(killer, new Totals(17_088, 0, 0, 0, 0));
        KillAnnouncer announcer = new KillAnnouncer(sets, id -> sets.standard().find("estatistico").orElseThrow(), tally);

        Text kill = announcer.killText(killer, "§aAlice", victim, "§cBob", false, false);
        Text bed = announcer.bedText(killer, "§aAlice", "§c", Text.of("Vermelho", "Red"));

        assertThat(kill.in(Language.PORTUGUESE)).isEqualTo("§7§cBob §7foi a §b17.089ª§7 kill de §aAlice§7.");
        assertThat(kill.in(Language.ENGLISH)).isEqualTo("§7§cBob §7was §aAlice§7's §b#17.089§7 kill.");
        assertThat(bed.in(Language.ENGLISH)).startsWith("§f§lBED DESTRUCTION §f» §7§cRed Bed");
        assertThat(bed.in(Language.PORTUGUESE)).startsWith("§f§lCAMA DESTRUÍDA §f» §7A §cCama do Vermelho");
    }

    private static Set<String> placeholders(String text) {
        Set<String> found = new LinkedHashSet<>();
        int start = text.indexOf('{');
        while (start >= 0) {
            int end = text.indexOf('}', start);
            if (end < 0) {
                break;
            }
            found.add(text.substring(start, end + 1));
            start = text.indexOf('{', end);
        }
        return found;
    }
}
