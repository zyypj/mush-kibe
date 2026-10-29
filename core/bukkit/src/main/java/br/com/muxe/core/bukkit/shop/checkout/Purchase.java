package br.com.muxe.core.bukkit.shop.checkout;

import br.com.muxe.core.bukkit.shop.catalog.ShopProduct;
import java.util.Objects;

public record Purchase(ShopProduct product, boolean fromPage) {

    public Purchase {
        Objects.requireNonNull(product, "product");
    }
}
