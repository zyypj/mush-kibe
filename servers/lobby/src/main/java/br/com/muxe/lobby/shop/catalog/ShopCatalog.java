package br.com.muxe.lobby.shop.catalog;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ShopCatalog(
    String titleTemplate,
    List<ShopCategory> categories,
    String defaultCategoryId,
    String featuredProductId,
    ItemBuilder pane,
    ItemBuilder currentPane
) {

    public ShopCatalog {
        Objects.requireNonNull(titleTemplate, "titleTemplate");
        categories = List.copyOf(categories);
        Objects.requireNonNull(pane, "pane");
        Objects.requireNonNull(currentPane, "currentPane");
        if (categories.stream().noneMatch(category -> category.id().equals(defaultCategoryId))) {
            throw new IllegalArgumentException("The default shop category '" + defaultCategoryId + "' does not exist");
        }
        Optional<ShopProduct> featured = categories.stream().flatMap(category -> category.products().stream())
            .filter(product -> product.id().equals(featuredProductId))
            .findFirst();
        if (featured.isEmpty() || featured.get().page().isEmpty()) {
            throw new IllegalArgumentException("The featured product '" + featuredProductId + "' must exist and have a page");
        }
    }

    public String title(String name) {
        return titleTemplate.replace("{name}", name);
    }

    public ShopCategory defaultCategory() {
        return category(defaultCategoryId).orElseThrow();
    }

    public ShopProduct featured() {
        return product(featuredProductId).orElseThrow();
    }

    public Optional<ShopCategory> category(String id) {
        return categories.stream().filter(category -> category.id().equals(id)).findFirst();
    }

    public Optional<ShopProduct> product(String id) {
        return categories.stream().flatMap(category -> category.products().stream()).filter(product -> product.id().equals(id)).findFirst();
    }
}
