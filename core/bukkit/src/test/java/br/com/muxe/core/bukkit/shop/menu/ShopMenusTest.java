package br.com.muxe.core.bukkit.shop.menu;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.bukkit.shop.catalog.ShopCatalog;
import br.com.muxe.core.bukkit.shop.catalog.ShopCategory;
import br.com.muxe.core.bukkit.shop.catalog.ShopProduct;
import br.com.muxe.core.bukkit.shop.checkout.LoadingRing;
import br.com.muxe.core.bukkit.shop.checkout.Purchase;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

class ShopMenusTest {

    private final ShopMenus menus = new ShopMenus(ShopFixtures.SETTINGS.catalog(), ShopFixtures.SETTINGS.checkout());
    private final ShopCatalog catalog = menus.catalog();
    private final Player player = ShopFixtures.player("tadeu");
    private final Recorder navigator = new Recorder();

    @Test
    void theCategoryMenuHighlightsTheCurrentTab() {
        Menu menu = menus.category(category("ranks"), "tadeu", navigator);

        assertThat(menu.title()).isEqualTo("Loja: Ranks");
        assertThat(menu.rows()).isEqualTo(6);
        assertThat(icon(menu, 1).isGlowing()).isTrue();
        assertThat(icon(menu, 2).isGlowing()).isFalse();
        assertThat(icon(menu, 3).material()).isEqualTo(Material.SKULL_ITEM);
        assertThat(icon(menu, 7).name()).isEqualTo("§aMeus pedidos");
        assertThat(icon(menu, 10).data()).isEqualTo((short) 5);
        for (int slot : List.of(9, 11, 12, 13, 14, 15, 16, 17)) {
            assertThat(icon(menu, slot).data()).isEqualTo((short) 7);
        }
        assertThat(icon(menu, 28).name()).isEqualTo("§aVIP");
        assertThat(icon(menu, 37).material()).isEqualTo(Material.PAPER);
    }

    @Test
    void theLimePaneFollowsTheSelectedTab() {
        Menu orders = menus.category(category("orders"), "tadeu", navigator);

        assertThat(orders.title()).isEqualTo("Loja: Meus pedidos");
        assertThat(icon(orders, 16).data()).isEqualTo((short) 5);
        assertThat(icon(orders, 10).data()).isEqualTo((short) 7);
        assertThat(icon(orders, 31).name()).isEqualTo("§cNenhum pedido encontrado!");
        assertThat(icon(orders, 7).isGlowing()).isTrue();
    }

    @Test
    void medalPreviewsUseThePlayerName() {
        Menu medals = menus.category(category("medals"), "tadeu", navigator);

        assertThat(icon(medals, 28).lore()).contains("§c§c火 §7tadeu: §fFogo");
    }

    @Test
    void tabsSwitchCategories() {
        Menu menu = menus.category(category("ranks"), "tadeu", navigator);

        click(menu, 3, ClickType.LEFT);

        assertThat(navigator.calls).containsExactly("category:credits");
    }

    @Test
    void onlyALeftClickOnAProductForSaleStartsTheCheckout() {
        Menu menu = menus.category(category("ranks"), "tadeu", navigator);

        click(menu, 28, ClickType.RIGHT);
        click(menu, 34, ClickType.LEFT);
        click(menu, 37, ClickType.LEFT);
        click(menu, 28, ClickType.LEFT);

        assertThat(navigator.calls).containsExactly("method:vip:false");
    }

    @Test
    void theProductPageGoesStraightToThePaymentMethods() {
        Menu page = menus.page(catalog.featurable("enderlore").orElseThrow(), navigator);

        assertThat(page.title()).isEqualTo("Loja: Enderlore");
        assertThat(page.rows()).isEqualTo(5);
        assertThat(icon(page, 4).lore()).endsWith("§eClique para ver a lista", "§ecompleta de vantagens!");
        assertThat(icon(page, 21).lore()).contains(" §a§5Enderlore", " §7Total: §0§aR$ 64,99");
        assertThat(icon(page, 23).name()).isEqualTo("§aCartão de Crédito");
        assertThat(icon(page, 40).lore()).containsExactly("§7Para Loja: Ranks");

        click(page, 21, ClickType.LEFT);
        click(page, 40, ClickType.LEFT);

        assertThat(navigator.calls).containsExactly("supporter:enderlore:true", "category:ranks");
    }

    @Test
    void theMethodMenuSummarizesTheOrder() {
        Menu method = menus.method(new Purchase(product("vip"), false), navigator);

        assertThat(method.title()).isEqualTo("Compra: Método de pagamento");
        assertThat(method.rows()).isEqualTo(4);
        assertThat(icon(method, 12).lore()).contains(" §a§aVIP", " §7Total: §0§aR$ 39,99");
        assertThat(icon(method, 31).name()).isEqualTo("§a§aVIP");
        assertThat(icon(method, 31).data()).isEqualTo((short) 10);
        assertThat(icon(method, 31).lore()).containsExactly(" ", "§7Total: §0§aR$ 39,99");
        assertThat(icon(method, 30).lore()).containsExactly("§7Para Loja: Ranks");

        click(method, 14, ClickType.LEFT);
        click(method, 30, ClickType.LEFT);

        assertThat(navigator.calls).containsExactly("supporter:vip:false", "category:ranks");
    }

    @Test
    void theSupporterMenuGoesBackToWhereTheCheckoutStarted() {
        Menu fromMethod = menus.supporter(new Purchase(product("medals-elements"), false), navigator);
        Menu fromPage = menus.supporter(new Purchase(catalog.featurable("enderlore").orElseThrow(), true), navigator);

        assertThat(fromMethod.title()).isEqualTo("Compra: Código de apoiador");
        assertThat(icon(fromMethod, 30).lore()).containsExactly("§7Para Compra: Método de pagamento");
        assertThat(icon(fromMethod, 31).name()).isEqualTo("§a§aPacote: Três Elementos");
        assertThat(icon(fromPage, 30).lore()).containsExactly("§7Para Loja: Enderlore");

        click(fromMethod, 30, ClickType.LEFT);
        click(fromPage, 30, ClickType.LEFT);
        click(fromPage, 12, ClickType.LEFT);
        click(fromPage, 14, ClickType.LEFT);

        assertThat(navigator.calls).containsExactly("method:medals-elements:false", "page:enderlore", "useCode:enderlore", "skipCode:enderlore");
    }

    @Test
    void theLoadingMenuLightsOnePaneOfTheRing() {
        Menu first = menus.loading(0, closed -> {
        });
        Menu next = menus.loading(1, closed -> {
        });

        assertThat(first.title()).isEqualTo("Compra: Carregando...");
        assertThat(first.rows()).isEqualTo(3);
        assertThat(first.buttons()).hasSize(8);
        assertThat(icon(first, 3).data()).isEqualTo((short) 5);
        assertThat(icon(first, 4).data()).isEqualTo((short) 7);
        assertThat(icon(next, 4).data()).isEqualTo((short) 5);
        assertThat(icon(next, LoadingRing.SLOTS.get(0)).data()).isEqualTo((short) 7);
        assertThat(icon(first, 13 - 1).name()).isEqualTo("§a...");
    }

    private ShopCategory category(String id) {
        return catalog.category(id).orElseThrow();
    }

    private ShopProduct product(String id) {
        return catalog.product(id).orElseThrow();
    }

    private static ItemBuilder icon(Menu menu, int slot) {
        return menu.button(slot).orElseThrow(() -> new AssertionError("No button at slot " + slot)).icon();
    }

    private void click(Menu menu, int slot, ClickType type) {
        menu.button(slot).orElseThrow().click().click(new ClickContext(player, type, slot, null));
    }

    private static final class Recorder implements ShopNavigator {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void category(Player player, ShopCategory category) {
            calls.add("category:" + category.id());
        }

        @Override
        public void page(Player player, ShopProduct product) {
            calls.add("page:" + product.id());
        }

        @Override
        public void method(Player player, Purchase purchase) {
            calls.add("method:" + purchase.product().id() + ":" + purchase.fromPage());
        }

        @Override
        public void supporter(Player player, Purchase purchase) {
            calls.add("supporter:" + purchase.product().id() + ":" + purchase.fromPage());
        }

        @Override
        public void useCode(Player player, Purchase purchase) {
            calls.add("useCode:" + purchase.product().id());
        }

        @Override
        public void skipCode(Player player, Purchase purchase) {
            calls.add("skipCode:" + purchase.product().id());
        }
    }
}
