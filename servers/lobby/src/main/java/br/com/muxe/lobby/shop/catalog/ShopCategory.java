package br.com.muxe.lobby.shop.catalog;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ShopCategory(String id, String name, int tabSlot, ItemBuilder tab, List<ShopProduct> products, Map<Integer, ItemBuilder> extras) {

    public ShopCategory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tab, "tab");
        products = List.copyOf(products);
        extras = Map.copyOf(extras);
    }
}
