package br.com.muxe.lobby.shop.catalog;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.Objects;

public record ProductPage(String name, ItemBuilder icon) {

    public ProductPage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
    }
}
