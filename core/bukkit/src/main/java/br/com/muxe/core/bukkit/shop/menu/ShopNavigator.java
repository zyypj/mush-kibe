package br.com.muxe.core.bukkit.shop.menu;

import br.com.muxe.core.bukkit.shop.catalog.ShopCategory;
import br.com.muxe.core.bukkit.shop.catalog.ShopProduct;
import br.com.muxe.core.bukkit.shop.checkout.Purchase;
import org.bukkit.entity.Player;

public interface ShopNavigator {

    void category(Player player, ShopCategory category);

    void page(Player player, ShopProduct product);

    void method(Player player, Purchase purchase);

    void supporter(Player player, Purchase purchase);

    void useCode(Player player, Purchase purchase);

    void skipCode(Player player, Purchase purchase);
}
