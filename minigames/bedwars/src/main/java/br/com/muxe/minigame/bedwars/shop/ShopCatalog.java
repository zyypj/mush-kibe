package br.com.muxe.minigame.bedwars.shop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

public record ShopCatalog(
    String quickBuyTitle,
    String categoryTitle,
    Map<String, Currency> currencies,
    Texts texts,
    List<Tab> tabs,
    List<String> quickBuy,
    Map<String, Category> categories,
    Map<String, ShopItem> items,
    Editor editor
) {

    public static final String QUICK_BUY = "compra-rapida";

    public static ShopCatalog from(ConfigurationSection shop) {
        Map<String, Currency> currencies = new LinkedHashMap<>();
        ConfigurationSection currencySection = section(shop, "currencies");
        for (String id : currencySection.getKeys(false)) {
            ConfigurationSection currency = section(currencySection, id);
            currencies.put(id, new Currency(id, material(currency.getString("material")), currency.getString("one"), currency.getString("many"), currency.getString("color", "&f")));
        }
        List<Tab> tabs = new ArrayList<>();
        for (Map<?, ?> tab : shop.getMapList("tabs")) {
            tabs.add(new Tab(((Number) tab.get("slot")).intValue(), String.valueOf(tab.get("category")), material(String.valueOf(tab.get("material"))), String.valueOf(tab.get("name"))));
        }
        Map<String, Category> categories = new LinkedHashMap<>();
        ConfigurationSection categorySection = section(shop, "categories");
        for (String id : categorySection.getKeys(false)) {
            ConfigurationSection category = section(categorySection, id);
            categories.put(id, new Category(id, category.getString("name", id), category.getStringList("items")));
        }
        Map<String, ShopItem> items = new LinkedHashMap<>();
        ConfigurationSection itemSection = section(shop, "items");
        for (String id : itemSection.getKeys(false)) {
            items.put(id, item(id, section(itemSection, id), currencies));
        }
        ConfigurationSection editor = section(shop, "hotbar-editor");
        ShopCatalog catalog = new ShopCatalog(
            shop.getString("quick-buy-title", "Compra Rápida"),
            shop.getString("category-title", "Loja: {category}"),
            currencies,
            new Texts(
                shop.getString("price"), shop.getString("level"), shop.getStringList("quick-buy-add"), shop.getStringList("quick-buy-remove"),
                shop.getString("cannot-afford"), shop.getString("can-afford"), shop.getString("maxed"), shop.getString("owned"),
                shop.getString("open-category"), shop.getString("name-affordable", "&a"), shop.getString("name-unaffordable", "&c"),
                shop.getStringList("tracker-locked")
            ),
            tabs,
            shop.getStringList("quick-buy"),
            categories,
            items,
            new Editor(editor.getInt("slot", 53), material(editor.getString("material")), editor.getString("name"), editor.getStringList("lore"))
        );
        catalog.validate();
        return catalog;
    }

    private void validate() {
        quickBuy.forEach(this::requireItem);
        categories.values().forEach(category -> category.items().forEach(this::requireItem));
    }

    private void requireItem(String id) {
        if (!items.containsKey(id)) {
            throw new IllegalArgumentException("The shop lists an unknown item: " + id);
        }
    }

    private static ShopItem item(String id, ConfigurationSection section, Map<String, Currency> currencies) {
        Type type = Type.valueOf(section.getString("type", "item").toUpperCase(Locale.ROOT));
        List<Tier> tiers = new ArrayList<>();
        for (Map<?, ?> tier : section.getMapList("tiers")) {
            tiers.add(new Tier(material(String.valueOf(tier.get("material"))), String.valueOf(tier.get("name")),
                ((Number) tier.get("price")).intValue(), currency(currencies, String.valueOf(tier.get("currency")))));
        }
        if (tiers.isEmpty()) {
            tiers.add(new Tier(material(section.getString("material", type == Type.POTION ? "POTION" : null)), section.getString("name", id), section.getInt("price"),
                currency(currencies, section.getString("currency"))));
        }
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        for (String entry : section.getStringList("enchantments")) {
            String[] parts = entry.split(":");
            enchantments.put(parts[0].toUpperCase(Locale.ROOT), parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
        }
        Potion potion = null;
        if (type == Type.POTION) {
            potion = new Potion(section.getString("effect", "").toUpperCase(Locale.ROOT), section.getInt("amplifier"), section.getInt("seconds"));
        }
        return new ShopItem(id, type, section.getInt("data", 0), section.getInt("amount", 1), section.getInt("tier", 0),
            section.getStringList("description"), enchantments, section.getBoolean("unbreakable", false), tiers, potion);
    }

    private static Currency currency(Map<String, Currency> currencies, String id) {
        return Objects.requireNonNull(currencies.get(id), "Unknown currency " + id);
    }

    private static Material material(String name) {
        return Objects.requireNonNull(Material.matchMaterial(String.valueOf(name)), "Unknown material " + name);
    }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return Objects.requireNonNull(parent.getConfigurationSection(path), "Missing configuration section " + path);
    }

    public enum Type {
        BLOCK,
        WOOL,
        SWORD,
        ITEM,
        ARMOR,
        SHEARS,
        PICKAXE,
        AXE,
        POTION,
        SPECIAL
    }

    public record Currency(String id, Material material, String one, String many, String color) {

        public String name(int amount) {
            return amount == 1 ? one : many;
        }
    }

    public record Tier(Material material, String name, int price, Currency currency) {
    }

    public record Potion(String effect, int amplifier, int seconds) {

        public PotionEffectType type() {
            return Objects.requireNonNull(PotionEffectType.getByName(effect), "Unknown potion effect " + effect);
        }
    }

    public record ShopItem(String id, Type type, int data, int amount, int armorTier, List<String> description,
                           Map<String, Integer> enchantments, boolean unbreakable, List<Tier> tiers, Potion potion) {

        public ShopItem {
            description = List.copyOf(description);
            enchantments = Map.copyOf(enchantments);
            tiers = List.copyOf(tiers);
        }

        public Map<Enchantment, Integer> resolvedEnchantments() {
            Map<Enchantment, Integer> resolved = new LinkedHashMap<>();
            enchantments.forEach((name, level) -> resolved.put(Objects.requireNonNull(Enchantment.getByName(name), "Unknown enchantment " + name), level));
            return resolved;
        }

        public boolean isTiered() {
            return type == Type.PICKAXE || type == Type.AXE;
        }
    }

    public record Tab(int slot, String category, Material material, String name) {
    }

    public record Category(String id, String name, List<String> items) {

        public Category {
            items = List.copyOf(items);
        }
    }

    public record Texts(String price, String level, List<String> quickBuyAdd, List<String> quickBuyRemove, String cannotAfford, String canAfford,
                        String maxed, String owned, String openCategory, String nameAffordable, String nameUnaffordable, List<String> trackerLocked) {
    }

    public record Editor(int slot, Material material, String name, List<String> lore) {
    }
}
