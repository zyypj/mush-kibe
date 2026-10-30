package br.com.muxe.minigame.bedwars.shop;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import br.com.muxe.minigame.bedwars.room.RoomPlayer;
import br.com.muxe.minigame.bedwars.shop.Offer.State;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.ShopItem;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShopCatalogTest {

    private final ShopCatalog catalog = ShopCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("shop"));
    private final RoomPlayer player = new RoomPlayer(UUID.randomUUID(), "tadeu");

    @Test
    void theQuickBuyKeepsTheExportedOrderAndEveryCategoryHasItsItems() {
        assertThat(catalog.quickBuy()).hasSize(21).startsWith("argila", "armadura-ferro", "tnt").endsWith("leite", "ovo-das-pontes");
        assertThat(catalog.categories().get("blocos").items()).containsExactly("la", "argila", "vidro", "pedra-do-fim", "escada", "madeira");
        assertThat(catalog.tabs()).extracting(ShopCatalog.Tab::slot).containsExactly(0, 1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    void anUnaffordableItemShowsTheExportedLore() {
        ShopItem clay = catalog.items().get("argila");
        Offer offer = Offer.of(clay, player);

        assertThat(offer.name(catalog.texts(), false)).isEqualTo("&cArgila");
        assertThat(offer.lore(catalog.texts(), false, true, false)).containsExactly(
            "&7Preço: &f12 Ferros",
            " ",
            "&7Bloco básico para proteger a sua",
            "&7cama.",
            " ",
            "&bClique segurando Shift para remover",
            "&bda compra rápida.",
            "&cVocê não tem Ferros o suficiente."
        );
    }

    @Test
    void itemsWithoutDescriptionGoStraightToTheQuickBuyLines() {
        Offer sword = Offer.of(catalog.items().get("espada-diamante"), player);

        assertThat(sword.lore(catalog.texts(), false, false, false)).containsExactly(
            "&7Preço: &24 Esmeraldas",
            " ",
            "&bClique segurando Shift para adicionar",
            "&bà compra rápida.",
            "&cVocê não tem Esmeraldas o suficiente."
        );
        assertThat(Offer.of(catalog.items().get("perola"), player).lore(catalog.texts(), false, true, false).getFirst()).isEqualTo("&7Preço: &22 Esmeraldas");
        assertThat(Offer.of(catalog.items().get("esponja"), player).lore(catalog.texts(), false, false, false).getFirst()).isEqualTo("&7Preço: &61 Ouro");
    }

    @Test
    void toolsClimbOneLevelPerPurchaseAndStopAtTheLast() {
        ShopItem pickaxe = catalog.items().get("picareta");

        Offer first = Offer.of(pickaxe, player);
        assertThat(first.tier().name()).isEqualTo("Picareta de Madeira (Eficiência I)");
        assertThat(first.lore(catalog.texts(), false, true, false)).startsWith("&7Preço: &f10 Ferros", "&7Nível: &eI", " ",
            "&7Você sempre renascerá com ao mínimo", "&7o último nível.");

        player.pickaxeTier(2);
        assertThat(Offer.of(pickaxe, player).tier().name()).isEqualTo("Picareta de Ouro (Eficiência III, Afiação II)");
        assertThat(Offer.of(pickaxe, player).level()).isEqualTo(3);

        player.pickaxeTier(4);
        assertThat(Offer.of(pickaxe, player).state()).isEqualTo(State.MAXED);
    }

    @Test
    void armorBelowWhatYouOwnCannotBeBoughtAgain() {
        player.armorTier(2);

        assertThat(Offer.of(catalog.items().get("armadura-malha"), player).state()).isEqualTo(State.OWNED);
        assertThat(Offer.of(catalog.items().get("armadura-ferro"), player).state()).isEqualTo(State.OWNED);
        assertThat(Offer.of(catalog.items().get("armadura-diamante"), player).state()).isEqualTo(State.AVAILABLE);
        assertThat(Offer.of(catalog.items().get("armadura-diamante"), player).boughtName()).isEqualTo("Armadura de Diamante");
    }
}
