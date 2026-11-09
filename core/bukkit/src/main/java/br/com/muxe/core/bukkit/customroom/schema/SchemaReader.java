package br.com.muxe.core.bukkit.customroom.schema;

import br.com.muxe.core.common.customroom.schema.Currency;
import br.com.muxe.core.common.customroom.schema.Icon;
import br.com.muxe.core.common.customroom.schema.OptionEntry;
import br.com.muxe.core.common.customroom.schema.OptionEntry.ChoiceValue;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Format;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Level;
import br.com.muxe.core.common.customroom.schema.OptionPage;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SchemaReader {

    private SchemaReader() {
    }

    public static RoomSchema parse(String game, String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalArgumentException("Invalid custom room schema of " + game, exception);
        }
        return read(game, configuration);
    }

    public static RoomSchema read(String game, ConfigurationSection section) {
        Map<String, Currency> currencies = new LinkedHashMap<>();
        ConfigurationSection currencySection = section.getConfigurationSection("currencies");
        if (currencySection != null) {
            for (String id : currencySection.getKeys(false)) {
                ConfigurationSection currency = currencySection.getConfigurationSection(id);
                currencies.put(id, new Currency(id, currency.getString("one", id), currency.getString("many", id), currency.getString("color", "&f"), icon(currency)));
            }
        }
        if (currencies.isEmpty()) {
            currencies.put("iron", new Currency("iron", "Ferro", "Ferros", "&f", Icon.of("IRON_INGOT")));
        }
        Map<String, OptionPage> pages = new LinkedHashMap<>();
        ConfigurationSection pageSection = section.getConfigurationSection("pages");
        if (pageSection != null) {
            for (String id : pageSection.getKeys(false)) {
                pages.put(id, page(id, pageSection.getConfigurationSection(id)));
            }
        }
        return new RoomSchema(game, section.getString("root", "options"), pages, currencies);
    }

    private static OptionPage page(String id, ConfigurationSection section) {
        List<OptionEntry> entries = new ArrayList<>();
        ConfigurationSection entrySection = section.getConfigurationSection("entries");
        if (entrySection != null) {
            for (String key : entrySection.getKeys(false)) {
                entries.add(entry(key, entrySection.getConfigurationSection(key)));
            }
        }
        return new OptionPage(id, section.getString("title", id), section.getInt("rows", 4), section.getInt("back-slot", -1), section.getString("back-to", ""),
            entries, turn(section.getConfigurationSection("previous")), turn(section.getConfigurationSection("next")));
    }

    private static Optional<OptionPage.Turn> turn(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        return Optional.of(new OptionPage.Turn(section.getInt("slot"), section.getString("page", ""), section.getInt("number", 1)));
    }

    private static OptionEntry entry(String id, ConfigurationSection section) {
        int slot = section.getInt("slot");
        String name = section.getString("name", id);
        List<String> description = section.getStringList("description");
        String key = section.getString("key", id);
        String type = section.getString("type", "link").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "link" -> new OptionEntry.Link(slot, icon(section), name, section.getStringList("lore"), section.getString("page", ""));
            case "stepper" -> new OptionEntry.Stepper(slot, icon(section), name, description, key, section.getString("default", "1"),
                section.getString("min", "0"), section.getString("max", "64"), section.getDouble("step", 1), format(section.getString("format", "integer")),
                section.getBoolean("amount-from-value", false));
            case "toggle" -> new OptionEntry.Toggle(slot, icon(section), name, description, key, section.getBoolean("default", false));
            case "decimal" -> new OptionEntry.Decimal(slot, icon(section), name, description, key, section.getString("default", "0"),
                section.getDouble("min", 0), section.getDouble("max", 10));
            case "choice" -> new OptionEntry.Choice(slot, name, description, key, values(section), section.getString("default", ""));
            case "timer" -> new OptionEntry.Timer(slot, icon(section), name, key, section.getInt("default", 0), section.getBoolean("toggleable", true));
            case "team" -> new OptionEntry.Team(slot, section.getInt("index", 1));
            case "shop-item" -> new OptionEntry.ShopItem(slot, key, icon(section), name, description, section.getInt("price", 0), section.getString("currency", "iron"));
            case "upgrade" -> new OptionEntry.Upgrade(slot, key, icon(section), name, description, levels(section), section.getBoolean("enabled", true));
            case "free" -> new OptionEntry.FreeAll(slot, section.getString("scope", key), section.getString("text", ""));
            default -> throw new IllegalArgumentException("Unknown custom room entry type " + type + " in " + section.getCurrentPath());
        };
    }

    private static List<ChoiceValue> values(ConfigurationSection section) {
        List<ChoiceValue> values = new ArrayList<>();
        ConfigurationSection valueSection = section.getConfigurationSection("values");
        if (valueSection == null) {
            return values;
        }
        Icon fallback = icon(section);
        for (String id : valueSection.getKeys(false)) {
            ConfigurationSection value = valueSection.getConfigurationSection(id);
            values.add(new ChoiceValue(id, value.getString("label", id), value.contains("material") ? icon(value) : fallback));
        }
        return values;
    }

    private static List<Level> levels(ConfigurationSection section) {
        List<Level> levels = new ArrayList<>();
        ConfigurationSection levelSection = section.getConfigurationSection("levels");
        if (levelSection == null) {
            return levels;
        }
        for (String id : levelSection.getKeys(false)) {
            ConfigurationSection level = levelSection.getConfigurationSection(id);
            levels.add(new Level(level.getString("label", ""), level.getInt("price", 0), level.getString("currency", "diamond")));
        }
        return levels;
    }

    private static Icon icon(ConfigurationSection section) {
        return new Icon(section.getString("material", "STONE").toUpperCase(Locale.ROOT), section.getInt("data", 0), section.getInt("amount", 1),
            section.getBoolean("glow", false), section.getString("texture", ""));
    }

    private static Format format(String text) {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "multiplier" -> Format.MULTIPLIER;
            case "seconds" -> Format.SECONDS;
            default -> Format.INTEGER;
        };
    }
}
