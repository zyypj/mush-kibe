package br.com.muxe.minigame.bedwars.shop;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import org.junit.jupiter.api.Test;

class ShopListingTest {

    private final ShopCatalog catalog = ShopCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("shop"));

    @Test
    void theQuickBuyIsNotACategoryAndListsNothingInAnyRoom() {
        assertThat(ShopService.listed(catalog, ShopCatalog.QUICK_BUY, false)).isEmpty();
        assertThat(ShopService.listed(catalog, ShopCatalog.QUICK_BUY, true)).isEmpty();
    }

    @Test
    void aCustomRoomAlsoListsTheCustomItemsOfTheCategory() {
        assertThat(ShopService.listed(catalog, "blocos", false)).doesNotContain("obsidian");
        assertThat(ShopService.listed(catalog, "blocos", true)).endsWith("obsidian");
    }
}
