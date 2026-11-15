package br.com.muxe.core.bukkit.stats;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.stats.StatsPages.Click;
import br.com.muxe.core.bukkit.stats.StatsPages.Shown;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.stats.domain.BedWarsTotals;
import br.com.muxe.core.common.stats.domain.ModeTotals;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class StatsPagesTest {

    private static final LocalizedConfig TEXTS = LocalizedConfig.bundled(StatsPagesTest.class, "config.yml");
    private static final StatsSettings PORTUGUESE = StatsSettings.from(TEXTS.in(Language.PORTUGUESE).getConfigurationSection("menus.statistics"));
    private static final StatsSettings ENGLISH = StatsSettings.from(TEXTS.in(Language.ENGLISH).getConfigurationSection("menus.statistics"));
    private static final Target SELF = new Target(UUID.randomUUID(), "tadeu", true);
    private static final Target OTHER = new Target(UUID.randomUUID(), "LoneRival", false);

    @Test
    void theHubListsTheTenGamesOfTheReferenceInTheirSlots() {
        List<Shown> hub = StatsPages.hub(PORTUGUESE.hub(), false);

        assertThat(hub).extracting(Shown::slot).containsExactly(10, 11, 12, 13, 14, 15, 16, 19, 20, 21);
        assertThat(hub).extracting(shown -> shown.icon().name()).containsExactly("§aHG", "§aDuels", "§aPvP", "§aSky Wars", "§aBed Wars", "§aQuick Builders",
            "§aCTF", "§aMurder", "§aBuild Battle", "§aHide and Seek");
        assertThat(hub).extracting(shown -> shown.icon().material()).containsExactly(Material.MUSHROOM_SOUP, Material.DIAMOND_SWORD, Material.IRON_CHESTPLATE,
            Material.EYE_OF_ENDER, Material.BED, Material.DIAMOND_PICKAXE, Material.WOOL, Material.REDSTONE, Material.WORKBENCH, Material.MELON_BLOCK);
        assertThat(hub.get(6).icon().data()).isEqualTo((short) 11);
        assertThat(hub).extracting(Shown::click).containsExactly(Click.NOTHING, Click.SOON, Click.NOTHING, Click.SOON, Click.BEDWARS, Click.NOTHING,
            Click.NOTHING, Click.NOTHING, Click.NOTHING, Click.NOTHING);
        assertThat(hub.getFirst().icon().lore()).contains("§7Vitórias Minimuxe: §a0", "§7Coins: §60").noneMatch(line -> line.toLowerCase().contains("mush"));
    }

    @Test
    void theHubOpenedFromMyProfileHasVoltarButOpenedByCommandHasNone() {
        assertThat(StatsPages.hub(PORTUGUESE.hub(), true)).filteredOn(shown -> shown.click() == Click.BACK).singleElement().satisfies(back -> {
            assertThat(back.slot()).isEqualTo(31);
            assertThat(back.icon().name()).isEqualTo("§aVoltar");
            assertThat(back.icon().lore()).containsExactly("§7Para Meu perfil");
        });
        assertThat(StatsPages.hub(PORTUGUESE.hub(), false)).noneMatch(shown -> shown.click() == Click.BACK);
    }

    @Test
    void theTitlesNameThePlayerWhoseStatisticsTheyAre() {
        assertThat(PORTUGUESE.hub().title(SELF)).isEqualTo("Suas estatísticas");
        assertThat(PORTUGUESE.hub().title(OTHER)).isEqualTo("Estatísticas de LoneRival");
        assertThat(PORTUGUESE.bedwars().title(SELF)).isEqualTo("Suas estatísticas (Bed Wars)");
        assertThat(PORTUGUESE.bedwars().title(OTHER)).isEqualTo("Estatísticas de LoneRival (Bed Wars)");
        assertThat(ENGLISH.hub().title(OTHER)).isEqualTo("LoneRival's statistics");
        assertThat(ENGLISH.bedwars().title(SELF)).isEqualTo("Your statistics (Bed Wars)");
    }

    @Test
    void theBedWarsPageShowsTheLifetimeCountersWithTheLevelBadgeAndDottedThousands() {
        ModeTotals general = new ModeTotals(8058, 11992, 4263, 17162, 12584, 1234, 19302, 2560, 1670, 5256, 2485, 0, 64, 4_424_587);
        ModeTotals solo = new ModeTotals(2144, 4814, 1501, 3421, 2614, 23, 5389, 793, 63, 1313, 829, 0, 14, 0);
        BedWarsTotals totals = new BedWarsTotals(Map.of(BedWarsTotals.GENERAL, general, "solo", solo));

        List<Shown> page = StatsPages.bedwars(PORTUGUESE.bedwars(), totals, true, "Suas estatísticas");

        assertThat(page).extracting(Shown::slot).containsExactly(4, 10, 12, 14, 16, 28, 30, 32, 34, 40, 49);
        assertThat(page.getFirst().icon().name()).isEqualTo("§aGeral");
        assertThat(page.getFirst().icon().lore()).containsExactly("§7Nível: §4[128§l❤§4]", "§7XP: §a4.424.587", " ", "§7Partidas: §a8.058", " ",
            "§7Camas quebradas: §a11.992", "§7Camas perdidas: §a4.263", " ", "§7Kills: §a17.162", "§7Mortes: §a12.584", "§7Assistências: §a1.234",
            "§7Kills finais: §a19.302", "§7Mortes finais: §a2.560", "§7Assistências finais: §a1.670", " ", "§7Vitórias: §a5.256", "§7Derrotas: §a2.485", " ",
            "§7Winstreak: §a0", "§7Maior Winstreak: §a64");
        assertThat(page.get(1).icon().name()).isEqualTo("§aSolo");
        assertThat(page.get(1).icon().lore()).startsWith("§7Partidas: §a2.144", " ", "§7Camas quebradas: §a4.814").endsWith("§7Maior Winstreak: §a14");
        assertThat(page.get(2).icon().amount()).isEqualTo(2);
        assertThat(page.get(2).icon().lore()).startsWith("§7Partidas: §a0");
        assertThat(page.get(9).icon().name()).isEqualTo("§aMega");
        assertThat(page.get(9).icon().amount()).isEqualTo(64);
        assertThat(page.getLast().click()).isEqualTo(Click.BACK);
        assertThat(page.getLast().icon().lore()).containsExactly("§7Para Suas estatísticas");
    }

    @Test
    void theBedWarsPageOpenedDirectlyHasNoVoltarAndAnUnknownPlayerShowsZeros() {
        List<Shown> page = StatsPages.bedwars(PORTUGUESE.bedwars(), BedWarsTotals.EMPTY, false, "Suas estatísticas");

        assertThat(page).noneMatch(shown -> shown.click() == Click.BACK);
        assertThat(page.getFirst().icon().lore()).startsWith("§7Nível: §7[1§l✫§7]", "§7XP: §a0");
    }
}
