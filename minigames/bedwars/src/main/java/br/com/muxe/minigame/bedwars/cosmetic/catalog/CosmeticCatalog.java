package br.com.muxe.minigame.bedwars.cosmetic.catalog;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.bukkit.configuration.ConfigurationSection;

public final class CosmeticCatalog {

    private final String category;
    private final Map<Integer, Long> prices;
    private final Map<String, BedCosmetic> cosmetics;

    public CosmeticCatalog(String category, Map<Integer, Long> prices, List<BedCosmetic> cosmetics) {
        this.category = category;
        this.prices = Map.copyOf(prices);
        Map<String, BedCosmetic> byId = new LinkedHashMap<>();
        cosmetics.forEach(cosmetic -> byId.put(cosmetic.id(), cosmetic));
        this.cosmetics = byId;
    }

    public static CosmeticCatalog from(ConfigurationSection section, Predicate<String> animated) {
        Map<Integer, Long> prices = new HashMap<>();
        ConfigurationSection priceSection = required(section, "prices");
        for (String stars : priceSection.getKeys(false)) {
            prices.put(Integer.parseInt(stars), priceSection.getLong(stars));
        }
        List<BedCosmetic> cosmetics = new ArrayList<>();
        ConfigurationSection list = required(section, "bed-destruction");
        for (String id : list.getKeys(false)) {
            ConfigurationSection entry = required(list, id);
            cosmetics.add(new BedCosmetic(
                id,
                entry.getString("name", id),
                ItemBuilder.from(required(entry, "icon")),
                entry.getInt("stars", 1),
                Optional.ofNullable(entry.getString("tag")),
                Availability.valueOf(entry.getString("availability", "EVENT").toUpperCase(Locale.ROOT)),
                entry.getStringList("description"),
                animated.test(id)
            ));
        }
        return new CosmeticCatalog(section.getString("category", "bedwars-cama"), prices, cosmetics);
    }

    public String category() {
        return category;
    }

    public List<BedCosmetic> all() {
        return List.copyOf(cosmetics.values());
    }

    public Optional<BedCosmetic> find(String id) {
        return Optional.ofNullable(cosmetics.get(id));
    }

    public long price(BedCosmetic cosmetic) {
        Long price = prices.get(cosmetic.stars());
        if (price == null) {
            throw new IllegalStateException("No price for " + cosmetic.stars() + " stars");
        }
        return price;
    }

    private static ConfigurationSection required(ConfigurationSection section, String path) {
        return Objects.requireNonNull(section.getConfigurationSection(path), "Missing configuration section " + section.getCurrentPath() + "." + path);
    }
}
