package br.com.muxe.minigame.bedwars.custom;

import br.com.muxe.core.bukkit.customroom.schema.SchemaReader;
import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.customroom.KnockbackPage;
import br.com.muxe.minigame.api.timeline.Timeline;
import br.com.muxe.minigame.bedwars.game.BedWarsEvent;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.ShopItem;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.Tier;
import br.com.muxe.minigame.bedwars.shop.UpgradeCatalog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class BedWarsRoomSchema {

    public static final List<Integer> LIST_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
    public static final int LIST_ROWS = 5;
    public static final int LIST_BACK = 39;
    public static final int LIST_NEXT = 41;
    public static final int LIST_FREE = 44;
    public static final String SHOP_PAGE = "shop-";
    public static final String UPGRADES_PAGE = "upgrades";
    public static final String KNOCKBACK_TEXTS = "knockback";

    private BedWarsRoomSchema() {
    }

    public static String build(ConfigurationSection custom, ShopCatalog shop, UpgradeCatalog upgrades, Timeline<BedWarsEvent> timeline, KnockbackProfile knockback) {
        return build(Localized.same(new Texts(custom, shop, upgrades)), timeline, knockback);
    }

    public static String build(Localized<Texts> texts, Timeline<BedWarsEvent> timeline, KnockbackProfile knockback) {
        YamlConfiguration yaml = tree(texts.standard(), timeline, knockback);
        for (Language language : Language.values()) {
            if (language == Language.DEFAULT) {
                continue;
            }
            YamlConfiguration translated = tree(texts.in(language), timeline, knockback);
            ConfigurationSection target = yaml.createSection(SchemaReader.TRANSLATIONS + "." + language.tag());
            for (String key : translated.getKeys(true)) {
                Object value = translated.get(key);
                if (!(value instanceof ConfigurationSection) && !Objects.equals(value, yaml.get(key))) {
                    target.set(key, value);
                }
            }
        }
        return yaml.saveToString();
    }

    private static YamlConfiguration tree(Texts texts, Timeline<BedWarsEvent> timeline, KnockbackProfile knockback) {
        ConfigurationSection custom = texts.custom();
        ShopCatalog shop = texts.shop();
        UpgradeCatalog upgrades = texts.upgrades();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("root", custom.getString("root", "options"));
        ConfigurationSection currencies = yaml.createSection("currencies");
        shop.currencies().values().forEach(currency -> {
            ConfigurationSection section = currencies.createSection(currency.id());
            section.set("one", currency.one());
            section.set("many", currency.many());
            section.set("color", currency.color());
            section.set("material", currency.material().name());
        });
        ConfigurationSection diamond = custom.getConfigurationSection("diamond-currency");
        if (diamond != null && !currencies.contains("diamond")) {
            ConfigurationSection section = currencies.createSection("diamond");
            diamond.getValues(false).forEach(section::set);
        }
        ConfigurationSection pages = yaml.createSection("pages");
        ConfigurationSection fixed = custom.getConfigurationSection("pages");
        if (fixed != null) {
            for (String id : fixed.getKeys(false)) {
                copy(fixed.getConfigurationSection(id), pages.createSection(id));
            }
        }
        events(pages, timeline);
        shopPages(pages, custom, shop);
        upgradesPage(pages, custom, upgrades);
        ConfigurationSection knockbackTexts = custom.getConfigurationSection(KNOCKBACK_TEXTS);
        KnockbackPage.write(pages, knockback, knockbackTexts == null ? new MemoryConfiguration() : knockbackTexts, 30, "options");
        return yaml;
    }

    public static List<Entry> shopEntries(ShopCatalog shop) {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ShopCatalog.Category category : shop.categories().values()) {
            for (String id : category.withCustom()) {
                ShopItem item = shop.items().get(id);
                if (item == null || !seen.add(id)) {
                    continue;
                }
                for (int index = 0; index < item.tiers().size(); index++) {
                    entries.add(new Entry(key(item, index), item, item.tiers().get(index)));
                }
            }
        }
        return entries;
    }

    public static String key(ShopItem item, int tierIndex) {
        return item.tiers().size() > 1 ? item.id() + "-" + (tierIndex + 1) : item.id();
    }

    public static String levelLabel(String label) {
        String text = label == null ? "" : label.replaceAll("&[0-9a-fk-or]", "");
        int start = text.indexOf(": ");
        int end = text.lastIndexOf(',');
        if (start < 0) {
            return "";
        }
        return (end > start ? text.substring(start + 2, end) : text.substring(start + 2)).trim();
    }

    private static void events(ConfigurationSection pages, Timeline<BedWarsEvent> timeline) {
        ConfigurationSection entries = pages.getConfigurationSection("events.entries");
        if (entries == null) {
            return;
        }
        List<Integer> intervals = CustomRules.intervals(timeline);
        for (int index = 0; index < CustomRules.EVENT_KEYS.size() && index < intervals.size(); index++) {
            ConfigurationSection entry = entries.getConfigurationSection(CustomRules.EVENT_KEYS.get(index));
            if (entry != null) {
                entry.set("default", intervals.get(index));
            }
        }
    }

    private static void shopPages(ConfigurationSection pages, ConfigurationSection custom, ShopCatalog shop) {
        List<Entry> entries = shopEntries(shop);
        int perPage = LIST_SLOTS.size();
        int count = Math.max(1, (entries.size() + perPage - 1) / perPage);
        String title = custom.getString("shop-page-title", "({page}/{pages})");
        for (int page = 1; page <= count; page++) {
            ConfigurationSection section = pages.createSection(SHOP_PAGE + page);
            section.set("title", title.replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(count)));
            section.set("rows", LIST_ROWS);
            section.set("back-slot", LIST_BACK);
            section.set("back-to", "options");
            if (page > 1) {
                turn(section.createSection("previous"), LIST_BACK, SHOP_PAGE + (page - 1), page - 1);
            }
            if (page < count) {
                turn(section.createSection("next"), LIST_NEXT, SHOP_PAGE + (page + 1), page + 1);
            }
            ConfigurationSection list = section.createSection("entries");
            List<Entry> shown = entries.subList((page - 1) * perPage, Math.min(entries.size(), page * perPage));
            for (int index = 0; index < shown.size(); index++) {
                Entry entry = shown.get(index);
                ConfigurationSection item = list.createSection(entry.key());
                item.set("type", "shop-item");
                item.set("slot", LIST_SLOTS.get(index));
                item.set("key", entry.key());
                item.set("material", entry.tier().material().name());
                boolean dataIcon = entry.item().type() == ShopCatalog.Type.POTION || entry.item().type() == ShopCatalog.Type.SPECIAL;
                item.set("data", dataIcon ? entry.item().data() : 0);
                item.set("amount", entry.item().amount());
                item.set("name", entry.tier().name());
                item.set("description", entry.item().description());
                item.set("price", entry.tier().price());
                item.set("currency", entry.tier().currency().id());
                item.set("enabled", entry.item().enabled());
            }
            ConfigurationSection free = list.createSection("free");
            free.set("type", "free");
            free.set("slot", LIST_FREE);
            free.set("scope", "shop");
            free.set("text", custom.getString("shop-free-text", ""));
        }
    }

    private static void upgradesPage(ConfigurationSection pages, ConfigurationSection custom, UpgradeCatalog upgrades) {
        ConfigurationSection section = pages.createSection(UPGRADES_PAGE);
        section.set("title", custom.getString("upgrades-page-title", UPGRADES_PAGE));
        section.set("rows", LIST_ROWS);
        section.set("back-slot", LIST_BACK);
        section.set("back-to", "options");
        ConfigurationSection list = section.createSection("entries");
        List<UpgradeCatalog.Entry> entries = upgrades.withCustom();
        for (int index = 0; index < entries.size() && index < LIST_SLOTS.size(); index++) {
            UpgradeCatalog.Entry entry = entries.get(index);
            ConfigurationSection item = list.createSection(entry.id());
            item.set("type", "upgrade");
            item.set("slot", LIST_SLOTS.get(index));
            item.set("key", entry.id());
            item.set("material", entry.material().name());
            item.set("name", entry.name());
            item.set("description", entry.description());
            item.set("enabled", entry.enabled());
            ConfigurationSection levels = item.createSection("levels");
            for (int level = 0; level < entry.tiers().size(); level++) {
                ConfigurationSection tier = levels.createSection(String.valueOf(level + 1));
                tier.set("label", levelLabel(entry.tiers().get(level).full()));
                tier.set("price", entry.tiers().get(level).price());
                tier.set("currency", "diamond");
            }
        }
        ConfigurationSection free = list.createSection("free");
        free.set("type", "free");
        free.set("slot", LIST_FREE);
        free.set("scope", "upgrades");
        free.set("text", custom.getString("upgrades-free-text", ""));
    }

    private static void turn(ConfigurationSection section, int slot, String page, int number) {
        section.set("slot", slot);
        section.set("page", page);
        section.set("number", number);
    }

    private static void copy(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            if (from.isConfigurationSection(key)) {
                copy(from.getConfigurationSection(key), to.createSection(key));
            } else {
                to.set(key, from.get(key));
            }
        }
    }

    public record Entry(String key, ShopItem item, Tier tier) {
    }

    public record Texts(ConfigurationSection custom, ShopCatalog shop, UpgradeCatalog upgrades) {
    }
}
