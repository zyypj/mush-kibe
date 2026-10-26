package br.com.muxe.lobby.shop;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.text.CenteredText;
import br.com.muxe.core.common.config.source.DurationParser;
import br.com.muxe.lobby.config.LobbySettings;
import br.com.muxe.lobby.shop.catalog.ProductPage;
import br.com.muxe.lobby.shop.catalog.ShopCatalog;
import br.com.muxe.lobby.shop.catalog.ShopCategory;
import br.com.muxe.lobby.shop.catalog.ShopProduct;
import br.com.muxe.lobby.shop.checkout.CheckoutSettings;
import br.com.muxe.lobby.shop.register.RegisterSettings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public record ShopSettings(ShopCatalog catalog, CheckoutSettings checkout, RegisterSettings register) {

    public static ShopSettings from(ConfigurationSection shop) {
        return new ShopSettings(catalog(shop), checkout(child(shop, "checkout")), RegisterSettings.from(child(shop, "register")));
    }

    private static ShopCatalog catalog(ConfigurationSection shop) {
        ConfigurationSection categories = child(shop, "categories");
        List<ShopCategory> parsed = new ArrayList<>();
        for (String id : categories.getKeys(false)) {
            parsed.add(category(id, child(categories, id)));
        }
        ConfigurationSection panes = child(shop, "panes");
        return new ShopCatalog(
            shop.getString("title", "Loja: {name}"),
            parsed,
            shop.getString("default-category", "ranks"),
            shop.getString("featured-product", ""),
            ItemBuilder.from(child(panes, "other")),
            ItemBuilder.from(child(panes, "current"))
        );
    }

    private static ShopCategory category(String id, ConfigurationSection section) {
        List<ShopProduct> products = new ArrayList<>();
        ConfigurationSection productSection = section.getConfigurationSection("products");
        if (productSection != null) {
            for (String productId : productSection.getKeys(false)) {
                products.add(product(productId, id, child(productSection, productId)));
            }
        }
        Map<Integer, ItemBuilder> extras = new LinkedHashMap<>();
        ConfigurationSection extraSection = section.getConfigurationSection("extras");
        if (extraSection != null) {
            for (String key : extraSection.getKeys(false)) {
                ConfigurationSection extra = child(extraSection, key);
                extras.put(extra.getInt("slot"), ItemBuilder.from(child(extra, "item")));
            }
        }
        return new ShopCategory(id, section.getString("name", id), section.getInt("tab-slot"), ItemBuilder.from(child(section, "tab")), products, extras);
    }

    private static ShopProduct product(String id, String category, ConfigurationSection section) {
        Optional<ProductPage> page = Optional.ofNullable(section.getConfigurationSection("page"))
            .map(pageSection -> new ProductPage(pageSection.getString("name", id), ItemBuilder.from(child(pageSection, "item"))));
        return new ShopProduct(
            id,
            category,
            section.getInt("slot"),
            ItemBuilder.from(child(section, "item")),
            section.getString("price", ""),
            section.getBoolean("purchasable", true),
            page
        );
    }

    private static CheckoutSettings checkout(ConfigurationSection section) {
        ConfigurationSection loading = child(section, "loading");
        return new CheckoutSettings(
            section.getString("method-title", "Compra: Método de pagamento"),
            section.getString("supporter-title", "Compra: Código de apoiador"),
            section.getString("loading-title", "Compra: Carregando..."),
            ItemBuilder.from(child(section, "pix")),
            ItemBuilder.from(child(section, "card")),
            ItemBuilder.from(child(section, "summary")),
            ItemBuilder.from(child(section, "back")),
            ItemBuilder.from(child(section, "use-code")),
            ItemBuilder.from(child(section, "skip-code")),
            ItemBuilder.from(child(loading, "lit")),
            ItemBuilder.from(child(loading, "dim")),
            LobbySettings.ticks(DurationParser.parse(loading.getString("step", "150ms"))),
            LobbySettings.ticks(DurationParser.parse(loading.getString("duration", "3s"))),
            DurationParser.parse(section.getString("code-timeout", "60s")),
            section.getString("payment-url", ""),
            section.getInt("chat-center", CenteredText.DEFAULT_CENTER)
        );
    }

    static ConfigurationSection child(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + parent.getCurrentPath() + "." + path);
    }
}
