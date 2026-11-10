package br.com.muxe.minigame.bedwars.shop;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.api.settings.HotbarItem;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import org.bukkit.Material;
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

    @Test
    void theRoomMenuShowsTheRoomItemInTheLastRowOfTheQuickBuy() {
        assertThat(ShopService.ROOM_MENU_SLOT).isEqualTo(45);
        assertThat(ShopService.roomMenuIcon(new HotbarItem(7, "BREWING_STAND_ITEM", "&aMenu da sala &7(/menu)")).material()).isEqualTo(Material.BREWING_STAND_ITEM);
        assertThat(ShopService.roomMenuIcon(new HotbarItem(7, "BREWING_STAND_ITEM", "&aMenu da sala &7(/menu)")).name()).isEqualTo("§aMenu da sala §7(/menu)");
    }
}
