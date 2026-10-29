package br.com.muxe.core.bukkit.shop.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuClick;
import br.com.muxe.core.bukkit.menu.MenuLayout;
import br.com.muxe.core.bukkit.shop.catalog.ProductPage;
import br.com.muxe.core.bukkit.shop.catalog.ShopCatalog;
import br.com.muxe.core.bukkit.shop.catalog.ShopCategory;
import br.com.muxe.core.bukkit.shop.catalog.ShopProduct;
import br.com.muxe.core.bukkit.shop.checkout.CheckoutSettings;
import br.com.muxe.core.bukkit.shop.checkout.LoadingRing;
import br.com.muxe.core.bukkit.shop.checkout.Purchase;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class ShopMenus {

    public static final int CATEGORY_ROWS = 6;
    public static final int PANE_ROW = 1;
    public static final int PAGE_ROWS = 5;
    public static final int PAGE_ICON = 4;
    public static final int PAGE_PIX = 21;
    public static final int PAGE_CARD = 23;
    public static final int PAGE_BACK = 40;
    public static final int CHECKOUT_ROWS = 4;
    public static final int CHECKOUT_LEFT = 12;
    public static final int CHECKOUT_RIGHT = 14;
    public static final int CHECKOUT_BACK = 30;
    public static final int CHECKOUT_SUMMARY = 31;

    private final ShopCatalog catalog;
    private final CheckoutSettings checkout;

    public ShopMenus(ShopCatalog catalog, CheckoutSettings checkout) {
        this.catalog = catalog;
        this.checkout = checkout;
    }

    public ShopCatalog catalog() {
        return catalog;
    }

    public Menu category(ShopCategory current, String playerName, ShopNavigator navigator) {
        Menu.Builder menu = Menu.builder(catalog.title(current.name())).rows(CATEGORY_ROWS);
        for (ShopCategory category : catalog.categories()) {
            menu.button(category.tabSlot(), category.tab().glow(category.id().equals(current.id())),
                click -> navigator.category(click.player(), category));
        }
        int currentColumn = current.tabSlot() % MenuLayout.ROW_WIDTH;
        for (int column = 0; column < MenuLayout.ROW_WIDTH; column++) {
            menu.button(PANE_ROW * MenuLayout.ROW_WIDTH + column, column == currentColumn ? catalog.currentPane() : catalog.pane());
        }
        current.extras().forEach(menu::button);
        for (ShopProduct product : current.products()) {
            menu.button(product.slot(), product.icon().replacing("player", playerName), click -> {
                if (product.purchasable() && click.isLeftClick()) {
                    navigator.method(click.player(), new Purchase(product, false));
                }
            });
        }
        return menu.build();
    }

    public Menu page(ShopProduct product, ShopNavigator navigator) {
        ShopCategory category = categoryOf(product);
        Purchase purchase = new Purchase(product, true);
        return Menu.builder(pageTitle(product)).rows(PAGE_ROWS)
            .button(PAGE_ICON, product.page().orElseThrow().icon())
            .button(PAGE_PIX, payment(checkout.pix(), product), click -> navigator.supporter(click.player(), purchase))
            .button(PAGE_CARD, payment(checkout.card(), product), click -> navigator.supporter(click.player(), purchase))
            .button(PAGE_BACK, back(catalog.title(category.name())), click -> navigator.category(click.player(), category))
            .build();
    }

    public Menu method(Purchase purchase, ShopNavigator navigator) {
        ShopCategory category = categoryOf(purchase.product());
        return Menu.builder(checkout.methodTitle()).rows(CHECKOUT_ROWS)
            .button(CHECKOUT_LEFT, payment(checkout.pix(), purchase.product()), click -> navigator.supporter(click.player(), purchase))
            .button(CHECKOUT_RIGHT, payment(checkout.card(), purchase.product()), click -> navigator.supporter(click.player(), purchase))
            .button(CHECKOUT_BACK, back(catalog.title(category.name())), click -> navigator.category(click.player(), category))
            .button(CHECKOUT_SUMMARY, summary(purchase.product()))
            .build();
    }

    public Menu supporter(Purchase purchase, ShopNavigator navigator) {
        String previous = purchase.fromPage() ? pageTitle(purchase.product()) : checkout.methodTitle();
        MenuClick back = purchase.fromPage()
            ? click -> navigator.page(click.player(), purchase.product())
            : click -> navigator.method(click.player(), purchase);
        return Menu.builder(checkout.supporterTitle()).rows(CHECKOUT_ROWS)
            .button(CHECKOUT_LEFT, checkout.useCode(), click -> navigator.useCode(click.player(), purchase))
            .button(CHECKOUT_RIGHT, checkout.skipCode(), click -> navigator.skipCode(click.player(), purchase))
            .button(CHECKOUT_BACK, back(previous), back)
            .button(CHECKOUT_SUMMARY, summary(purchase.product()))
            .build();
    }

    public Menu loading(long step, Consumer<Player> onClose) {
        Menu.Builder menu = Menu.builder(checkout.loadingTitle()).rows(LoadingRing.ROWS).onClose(onClose);
        int lit = LoadingRing.litSlot(step);
        for (int slot : LoadingRing.SLOTS) {
            menu.button(slot, slot == lit ? checkout.litPane() : checkout.dimPane());
        }
        return menu.build();
    }

    public String pageTitle(ShopProduct product) {
        return catalog.title(product.page().map(ProductPage::name).orElse(product.id()));
    }

    private ShopCategory categoryOf(ShopProduct product) {
        return catalog.category(product.category()).orElseThrow();
    }

    private ItemBuilder payment(ItemBuilder method, ShopProduct product) {
        return method.replacing("product", product.displayName(), "price", product.price());
    }

    private ItemBuilder summary(ShopProduct product) {
        ItemBuilder icon = product.icon();
        return checkout.summary()
            .material(icon.material())
            .data(icon.data())
            .glow(icon.isGlowing())
            .skullTexture(icon.skullTexture())
            .replacing("product", product.displayName(), "price", product.price());
    }

    private ItemBuilder back(String menuTitle) {
        return checkout.back().replacing("menu", menuTitle);
    }
}
