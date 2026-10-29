package br.com.muxe.core.bukkit.shop.catalog;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.Objects;
import java.util.Optional;

public record ShopProduct(String id, String category, int slot, ItemBuilder icon, String price, boolean purchasable, Optional<ProductPage> page) {

    public ShopProduct {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(page, "page");
    }

    public String displayName() {
        return icon.name() == null ? id : icon.name();
    }
}
