package br.com.muxe.minigame.bedwars;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

class BedWarsTranslationsTest {

    private static final Set<String> COLOR_CODES_THAT_LOOK_READABLE = Set.of(
        "custom-room.diamond-currency.color",
        "levels.rainbow.digits",
        "tnt-timer.colors.yellow",
        "tnt-timer.colors.orange",
        "tnt-timer.colors.red",
        "shop.currencies.iron.color",
        "shop.currencies.diamond.color",
        "shop.name-affordable",
        "shop.name-unaffordable",
        "upgrades.name-unaffordable",
        "upgrades.name-affordable",
        "upgrades.name-maxed",
        "cosmetics.menus.bed-destruction.lore.stars-on",
        "cosmetics.menus.bed-destruction.lore.stars-off",
        "end-titles.victory.highlight",
        "end-titles.victory.base",
        "end-titles.defeat.accent",
        "end-titles.defeat.highlight",
        "end-titles.defeat.base",
        "lobby.blink-colors"
    );

    private final LocalizedConfig texts = LocalizedConfig.bundled(BedWarsPlugin.class, "config.yml");

    @Test
    void everyReadableTextOfTheBedWarsConfigHasAnEnglishVersion() {
        Set<String> missing = new LinkedHashSet<>(texts.untranslated(Language.ENGLISH, ""));
        missing.removeAll(COLOR_CODES_THAT_LOOK_READABLE);

        assertThat(missing).isEmpty();
    }

    @Test
    void theAllowedPathsHoldNothingButColourCodes() {
        for (String path : COLOR_CODES_THAT_LOOK_READABLE) {
            Object value = texts.standard().get(path);
            List<?> values = value instanceof List<?> list ? list : List.of(String.valueOf(value));

            assertThat(values).as(path).isNotEmpty().allMatch(item -> String.valueOf(item).matches("(&[0-9a-fk-or])+"));
        }
    }

    @Test
    void theListsOfTheShopAndTheUpgradesAreTranslatedItemByItem() {
        ConfigurationSection english = texts.in(Language.ENGLISH);
        ConfigurationSection portuguese = texts.in(Language.PORTUGUESE);

        assertThat(english.getMapList("shop.tabs").getFirst().get("name")).isEqualTo("&aQuick Buy");
        assertThat(english.getMapList("shop.tabs").getFirst().get("category")).isEqualTo(portuguese.getMapList("shop.tabs").getFirst().get("category"));
        assertThat(english.getMapList("upgrades.list.fadiga.tiers").get(1).get("extra")).isEqualTo("&7&b2 Diamonds");
        assertThat(english.getMapList("upgrades.list.fadiga.tiers").get(1).get("price")).isEqualTo(2);
        assertThat(english.getMapList("upgrades.list.regeneracao.tiers")).isEqualTo(portuguese.getMapList("upgrades.list.regeneracao.tiers"));
    }

    @Test
    void theEnglishTextsKeepThePlaceholdersOfThePortugueseOnes() {
        ConfigurationSection portuguese = texts.in(Language.PORTUGUESE);
        ConfigurationSection english = texts.in(Language.ENGLISH);
        List<String> different = new ArrayList<>();
        for (String path : portuguese.getKeys(true)) {
            Object original = portuguese.get(path);
            Object translated = english.get(path);
            if (!placeholders(original).equals(placeholders(translated))) {
                different.add(path + ": " + placeholders(original) + " / " + placeholders(translated));
            }
        }

        assertThat(different).isEmpty();
    }

    private static Set<String> placeholders(Object value) {
        Set<String> found = new LinkedHashSet<>();
        if (value instanceof String text) {
            int start = text.indexOf('{');
            while (start >= 0) {
                int end = text.indexOf('}', start);
                if (end < 0) {
                    break;
                }
                found.add(text.substring(start, end + 1));
                start = text.indexOf('{', end);
            }
        } else if (value instanceof List<?> items) {
            items.forEach(item -> found.addAll(placeholders(item)));
        } else if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> found.addAll(placeholders(item)));
        }
        return found;
    }
}
