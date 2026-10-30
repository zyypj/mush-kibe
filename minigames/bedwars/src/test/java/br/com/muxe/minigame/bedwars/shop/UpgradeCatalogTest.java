package br.com.muxe.minigame.bedwars.shop;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog.Entry;
import org.junit.jupiter.api.Test;

class UpgradeCatalogTest {

    private final UpgradeCatalog catalog = UpgradeCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("upgrades"));

    @Test
    void theSevenUpgradesSitInTheExportedSlots() {
        assertThat(catalog.entries()).extracting(Entry::slot).containsExactly(10, 11, 12, 13, 14, 15, 16);
        assertThat(catalog.title()).isEqualTo("Melhorias do Time");
    }

    @Test
    void theDefaultMenuShowsEveryTierMissingAndTheNextNumeral() {
        Entry armor = entry("reforco");

        assertThat(armor.name(catalog.texts(), 0, 0)).isEqualTo("&cReforço na Armadura I");
        assertThat(armor.lore(catalog.texts(), 0, 0)).containsExactly(
            "&7O seu time ganha Proteção",
            "&7em todas as peças armaduras.",
            " ",
            "&c✗ &7Nível 1: Proteção I, &b2 Diamantes",
            "&c✗ &7Nível 2: Proteção II, &b4 Diamantes",
            "&c✗ &7Nível 3: Proteção III, &b8 Diamantes",
            "&c✗ &7Nível 4: Proteção IV, &b16 Diamantes",
            " ",
            "&cVocê não tem Diamantes o suficiente."
        );
    }

    @Test
    void aBoughtTierIsCheckedAndTheNameMovesToTheNextOne() {
        Entry armor = entry("reforco");

        assertThat(armor.name(catalog.texts(), 1, 0)).isEqualTo("&cReforço na Armadura II");
        assertThat(armor.lore(catalog.texts(), 1, 0)).contains("&a&l✓ &7Nível 1: Proteção I, &b2 Diamantes", "&c✗ &7Nível 2: Proteção II, &b4 Diamantes");
        assertThat(armor.boughtName(1)).isEqualTo("Reforço na Armadura I");
    }

    @Test
    void enoughDiamondsTurnTheNameYellowAndAMaxedUpgradeGreen() {
        Entry heal = entry("regeneracao");

        assertThat(heal.name(catalog.texts(), 0, 1)).isEqualTo("&eRegeneração na Ilha");
        assertThat(heal.lore(catalog.texts(), 0, 1)).containsExactly(
            "&7Crie um campo de Regeneração", "&7em volta de sua ilha.", " ", "&7Preço: &b1 Diamante", " ", "&eClique para melhorar.");
        assertThat(heal.name(catalog.texts(), 1, 0)).isEqualTo("&aRegeneração na Ilha");
        assertThat(heal.lore(catalog.texts(), 1, 0).getLast()).isEqualTo("&aMelhorado ao máximo.");
    }

    @Test
    void fatigueKeepsTheExportedLineWrap() {
        assertThat(entry("fadiga").lore(catalog.texts(), 0, 0)).containsExactly(
            "&7Cause Fadiga em invasores.",
            " ",
            "&c✗ &7Nível 1: Fadiga I por 7 segundos, &b1",
            "&bDiamante",
            "&c✗ &7Nível 2: Fadiga I por 10 segundos,",
            "&7&b2 Diamantes",
            " ",
            "&cVocê não tem Diamantes o suficiente."
        );
        assertThat(entry("fadiga").name(catalog.texts(), 0, 0)).isEqualTo("&cFadiga I");
    }

    private Entry entry(String id) {
        return catalog.entries().stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow();
    }
}
