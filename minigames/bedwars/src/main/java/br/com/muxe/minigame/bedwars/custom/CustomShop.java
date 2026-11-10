package br.com.muxe.minigame.bedwars.custom;

import br.com.muxe.core.common.customroom.schema.RoomEditor.LimitType;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Currency;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.ShopItem;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tier;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog.Cost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class CustomShop {

    public static final String DIAMOND = "diamond";
    public static final String DIAMOND_COLOR = "&b";

    private final CustomRules rules;
    private final Map<String, Integer> purchases = new HashMap<>();

    public CustomShop(CustomRules rules) {
        this.rules = rules;
    }

    public Optional<Tier> tier(ShopItem item, Tier tier, Map<String, Currency> currencies) {
        if (!rules.custom()) {
            return item.enabled() ? Optional.of(tier) : Optional.empty();
        }
        String key = BedWarsRoomSchema.key(item, Math.max(0, item.tiers().indexOf(tier)));
        if (!rules.sells(key, item.enabled())) {
            return Optional.empty();
        }
        Currency currency = Optional.ofNullable(currencies.get(rules.currency(key, tier.currency().id()))).orElse(tier.currency());
        return Optional.of(new Tier(tier.material(), tier.name(), rules.price(key, tier.price()), currency));
    }

    public Optional<UpgradeCatalog.Entry> upgrade(UpgradeCatalog.Entry entry, Map<String, Currency> currencies, UpgradeCatalog.Texts texts) {
        if (!rules.custom()) {
            return entry.enabled() ? Optional.of(entry) : Optional.empty();
        }
        if (!rules.upgrades(entry.id(), entry.enabled())) {
            return Optional.empty();
        }
        List<Integer> prices = new ArrayList<>();
        List<Cost> costs = new ArrayList<>();
        for (int level = 1; level <= entry.tiers().size(); level++) {
            int price = rules.upgradePrice(entry.id(), level, entry.tiers().get(level - 1).price());
            Currency currency = Optional.ofNullable(currencies.get(rules.upgradeCurrency(entry.id(), level, DIAMOND)))
                .or(() -> Optional.ofNullable(currencies.get(DIAMOND)))
                .orElseGet(() -> new Currency(DIAMOND, Material.DIAMOND, texts.diamondOne(), texts.diamondMany(), DIAMOND_COLOR));
            prices.add(price);
            costs.add(new Cost(currency.material(), price == 0 ? texts.free() : currency.color() + price + " " + currency.name(price)));
        }
        return Optional.of(entry.priced(prices, costs));
    }

    public boolean allows(ShopItem item, Tier tier, String team, String player, int inInventory) {
        if (!rules.custom()) {
            return true;
        }
        String key = BedWarsRoomSchema.key(item, Math.max(0, item.tiers().indexOf(tier)));
        int limit = rules.limit(key);
        return switch (rules.limitType(key)) {
            case NONE -> true;
            case TEAM -> purchases.getOrDefault("team:" + team + ":" + key, 0) < limit;
            case PLAYER -> purchases.getOrDefault("player:" + player + ":" + key, 0) < limit;
            case INVENTORY -> inInventory < limit;
        };
    }

    public void bought(ShopItem item, Tier tier, String team, String player) {
        String key = BedWarsRoomSchema.key(item, Math.max(0, item.tiers().indexOf(tier)));
        purchases.merge("team:" + team + ":" + key, 1, Integer::sum);
        purchases.merge("player:" + player + ":" + key, 1, Integer::sum);
    }

    public LimitType limitType(ShopItem item, Tier tier) {
        return rules.limitType(BedWarsRoomSchema.key(item, Math.max(0, item.tiers().indexOf(tier))));
    }
}
