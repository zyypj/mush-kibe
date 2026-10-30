package br.com.muxe.minigame.bedwars.shop;

import br.com.muxe.minigame.bedwars.room.RoomPlayer;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.ShopItem;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Texts;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tier;
import java.util.ArrayList;
import java.util.List;

public record Offer(ShopItem item, State state, Tier tier, int level) {

    public enum State {
        AVAILABLE,
        OWNED,
        MAXED
    }

    public static Offer of(ShopItem item, RoomPlayer player) {
        List<Tier> tiers = item.tiers();
        return switch (item.type()) {
            case PICKAXE -> tiered(item, player.pickaxeTier());
            case AXE -> tiered(item, player.axeTier());
            case ARMOR -> new Offer(item, player.armorTier() >= item.armorTier() ? State.OWNED : State.AVAILABLE, tiers.getFirst(), 0);
            case SHEARS -> new Offer(item, player.hasShears() ? State.OWNED : State.AVAILABLE, tiers.getFirst(), 0);
            default -> new Offer(item, State.AVAILABLE, tiers.getFirst(), 0);
        };
    }

    private static Offer tiered(ShopItem item, int owned) {
        if (owned >= item.tiers().size()) {
            return new Offer(item, State.MAXED, item.tiers().getLast(), item.tiers().size());
        }
        return new Offer(item, State.AVAILABLE, item.tiers().get(owned), owned + 1);
    }

    public String name(Texts texts, boolean affordable) {
        String color = state == State.AVAILABLE && !affordable ? texts.nameUnaffordable() : texts.nameAffordable();
        return color + tier.name();
    }

    public List<String> lore(Texts texts, boolean affordable, boolean inQuickBuy, boolean locked) {
        List<String> lore = new ArrayList<>();
        lore.add(texts.price()
            .replace("{color}", tier.currency().color())
            .replace("{amount}", String.valueOf(tier.price()))
            .replace("{currency}", tier.currency().name(tier.price())));
        if (item.isTiered()) {
            lore.add(texts.level().replace("{level}", BedWarsSettings.roman(Math.max(1, level))));
        }
        lore.add(" ");
        if (!item.description().isEmpty()) {
            lore.addAll(item.description());
            lore.add(" ");
        }
        lore.addAll(inQuickBuy ? texts.quickBuyRemove() : texts.quickBuyAdd());
        if (locked) {
            lore.addAll(texts.trackerLocked());
        } else if (state == State.MAXED) {
            lore.add(texts.maxed());
        } else if (state == State.OWNED) {
            lore.add(texts.owned());
        } else if (affordable) {
            lore.add(texts.canAfford());
        } else {
            lore.add(texts.cannotAfford().replace("{currency}", tier.currency().many()));
        }
        return lore;
    }

    public String boughtName() {
        return tier.name().replace(" (Permanente)", "");
    }
}
