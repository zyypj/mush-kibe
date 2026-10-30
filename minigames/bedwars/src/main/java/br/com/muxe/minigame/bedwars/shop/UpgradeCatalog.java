package br.com.muxe.minigame.bedwars.shop;

import br.com.muxe.minigame.bedwars.room.RoomTeam.Upgrade;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record UpgradeCatalog(String title, Texts texts, List<Entry> entries) {

    public static UpgradeCatalog from(ConfigurationSection section) {
        List<Entry> entries = new ArrayList<>();
        ConfigurationSection list = Objects.requireNonNull(section.getConfigurationSection("list"), "Missing upgrades.list");
        for (String id : list.getKeys(false)) {
            ConfigurationSection entry = Objects.requireNonNull(list.getConfigurationSection(id));
            List<Tier> tiers = new ArrayList<>();
            for (Map<?, ?> tier : entry.getMapList("tiers")) {
                Object extra = tier.get("extra");
                tiers.add(new Tier(String.valueOf(tier.get("label")), extra == null ? null : String.valueOf(extra), ((Number) tier.get("price")).intValue()));
            }
            entries.add(new Entry(
                id,
                entry.getInt("slot"),
                Objects.requireNonNull(Material.matchMaterial(entry.getString("material", "")), "Unknown material in upgrade " + id),
                entry.getString("name", id),
                Upgrade.valueOf(entry.getString("upgrade", "").toUpperCase(Locale.ROOT)),
                entry.getBoolean("single", false),
                entry.getStringList("description"),
                tiers
            ));
        }
        return new UpgradeCatalog(section.getString("title", "Melhorias do Time"), new Texts(
            section.getString("tier-bought"), section.getString("tier-missing"), section.getString("price"),
            section.getString("diamond-one"), section.getString("diamond-many"), section.getString("cannot-afford"),
            section.getString("can-afford"), section.getString("maxed"), section.getString("name-unaffordable"),
            section.getString("name-affordable"), section.getString("name-maxed")
        ), entries);
    }

    public record Texts(String bought, String missing, String price, String diamondOne, String diamondMany, String cannotAfford,
                        String canAfford, String maxed, String nameUnaffordable, String nameAffordable, String nameMaxed) {
    }

    public record Tier(String label, String extra, int price) {
    }

    public record Entry(String id, int slot, Material material, String name, Upgrade upgrade, boolean single, List<String> description, List<Tier> tiers) {

        public Entry {
            description = List.copyOf(description);
            tiers = List.copyOf(tiers);
        }

        public boolean maxed(int level) {
            return level >= tiers.size();
        }

        public int nextPrice(int level) {
            return tiers.get(Math.min(level, tiers.size() - 1)).price();
        }

        public String name(Texts texts, int level, int diamonds) {
            String color = maxed(level) ? texts.nameMaxed() : diamonds >= nextPrice(level) ? texts.nameAffordable() : texts.nameUnaffordable();
            return color + boughtName(Math.min(level + 1, tiers.size()));
        }

        public String boughtName(int level) {
            return single ? name : name + " " + BedWarsSettings.roman(level);
        }

        public List<String> lore(Texts texts, int level, int diamonds) {
            List<String> lore = new ArrayList<>(description);
            lore.add(" ");
            if (single) {
                int price = tiers.getFirst().price();
                lore.add(texts.price().replace("{amount}", String.valueOf(price)).replace("{currency}", price == 1 ? texts.diamondOne() : texts.diamondMany()));
            } else {
                for (int index = 0; index < tiers.size(); index++) {
                    Tier tier = tiers.get(index);
                    lore.add((index < level ? texts.bought() : texts.missing()) + tier.label());
                    if (tier.extra() != null) {
                        lore.add(tier.extra());
                    }
                }
            }
            lore.add(" ");
            if (maxed(level)) {
                lore.add(texts.maxed());
            } else if (diamonds >= nextPrice(level)) {
                lore.add(texts.canAfford());
            } else {
                lore.add(texts.cannotAfford());
            }
            return lore;
        }
    }
}
