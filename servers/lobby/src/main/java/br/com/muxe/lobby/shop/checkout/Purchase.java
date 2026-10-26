package br.com.muxe.lobby.shop.checkout;

import br.com.muxe.lobby.shop.catalog.ShopProduct;
import java.util.Objects;

public record Purchase(ShopProduct product, boolean fromPage) {

    public Purchase {
        Objects.requireNonNull(product, "product");
    }
}
