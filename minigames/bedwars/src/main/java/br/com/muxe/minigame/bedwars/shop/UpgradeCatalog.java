package br.com.muxe.minigame.bedwars.shop;

import br.com.muxe.minigame.bedwars.room.RoomTeam.Upgrade;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record UpgradeCatalog(String title, Texts texts, List<Entry> entries, List<Entry> customEntries) {

    public UpgradeCatalog {
        entries = List.copyOf(entries);
        customEntries = List.copyOf(customEntries);
    }

    public List<Entry> withCustom() {
        List<Entry> all = new ArrayList<>(entries);
        all.addAll(customEntries);
        return List.copyOf(all);
    }

    public static int rows(Collection<Entry> shown) {
        int last = shown.stream().mapToInt(Entry::slot).max().orElse(0);
        return Math.clamp(last / 9 + 2, MIN_ROWS, MAX_ROWS);
    }

    public static UpgradeCatalog from(ConfigurationSection section) {
        List<Entry> entries = entries(Objects.requireNonNull(section.getConfigurationSection("list"), "Missing upgrades.list"));
        ConfigurationSection custom = section.getConfigurationSection("custom-list");
        List<Entry> customEntries = custom == null ? List.of() : entries(custom);
        return new UpgradeCatalog(section.getString("title", ""), new Texts(
            section.getString("tier-bought"), section.getString("tier-missing"), section.getString("price"),
            section.getString("diamond-one"), section.getString("diamond-many"), section.getString("cannot-afford"),
            section.getString("can-afford"), section.getString("maxed"), section.getString("name-unaffordable"),
            section.getString("name-affordable"), section.getString("name-maxed"), section.getString("free", "")
        ), entries, customEntries);
    }

    private static List<Entry> entries(ConfigurationSection list) {
        List<Entry> entries = new ArrayList<>();
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
                tiers,
                List.of(),
                entry.getBoolean("enabled", true)
            ));
        }
        return entries;
    }

    public record Texts(String bought, String missing, String price, String diamondOne, String diamondMany, String cannotAfford,
                        String canAfford, String maxed, String nameUnaffordable, String nameAffordable, String nameMaxed, String free) {
    }

    public static final int LINE_WIDTH = 40;
    public static final int MIN_ROWS = 3;
    public static final int MAX_ROWS = 6;

    public record Tier(String label, String extra, int price) {

        public String full() {
            return extra == null ? label : label + " " + extra.replaceAll("^(&[0-9a-fk-or])+", "");
        }
    }

    public record Cost(Material material, String text) {
    }

    public Optional<Entry> entry(String id) {
        return withCustom().stream().filter(entry -> entry.id().equals(id)).findFirst();
    }

    public record Entry(String id, int slot, Material material, String name, Upgrade upgrade, boolean single, List<String> description, List<Tier> tiers,
                        List<Cost> costs, boolean enabled) {

        public Entry {
            description = List.copyOf(description);
            tiers = List.copyOf(tiers);
            costs = List.copyOf(costs);
        }

        public Entry priced(List<Integer> prices, List<Cost> changedCosts) {
            List<Tier> changed = new ArrayList<>();
            for (int index = 0; index < tiers.size(); index++) {
                Tier tier = tiers.get(index);
                if (single) {
                    changed.add(new Tier(tier.label(), tier.extra(), prices.get(index)));
                    continue;
                }
                String full = tier.full();
                int comma = full.lastIndexOf(',');
                List<String> lines = wrap((comma < 0 ? full : full.substring(0, comma)) + ", " + changedCosts.get(index).text());
                changed.add(new Tier(lines.getFirst(), lines.size() > 1 ? lines.get(1) : null, prices.get(index)));
            }
            return new Entry(id, slot, material, name, upgrade, single, description, changed, changedCosts, enabled);
        }

        public static List<String> wrap(String line) {
            if (visible(line) <= LINE_WIDTH) {
                return List.of(line);
            }
            int split = -1;
            for (int index = line.indexOf(','); index >= 0; index = line.indexOf(',', index + 1)) {
                if (visible(line.substring(0, index + 1)) <= LINE_WIDTH) {
                    split = index;
                }
            }
            if (split < 0) {
                return List.of(line);
            }
            return List.of(line.substring(0, split + 1), "&7" + line.substring(split + 1).trim());
        }

        private static int visible(String text) {
            return text.replaceAll("&[0-9a-fk-or]", "").length();
        }

        public Material currency(int level) {
            return costs.isEmpty() ? Material.DIAMOND : costs.get(Math.min(level, costs.size() - 1)).material();
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
            if (single && !costs.isEmpty()) {
                lore.add(texts.price().replace("&b{amount} {currency}", costs.getFirst().text()));
            } else if (single) {
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
