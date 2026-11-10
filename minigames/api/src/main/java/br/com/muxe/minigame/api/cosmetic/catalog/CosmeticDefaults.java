package br.com.muxe.minigame.api.cosmetic.catalog;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class CosmeticDefaults {

    public static final String RESOURCE = "/cosmetics.yml";
    public static final String TRANSLATIONS = "/lang/cosmetics-";
    public static final String KILL_MESSAGES = "kill-messages";
    public static final String LIST = "cosmetics";
    public static final String HIDDEN = "hidden";

    private final ConfigurationSection defaults;

    private CosmeticDefaults(ConfigurationSection defaults) {
        this.defaults = defaults;
    }

    public static CosmeticDefaults load() {
        return new CosmeticDefaults(Objects.requireNonNull(read(RESOURCE), "Missing " + RESOURCE));
    }

    public static CosmeticDefaults load(Language language) {
        return new CosmeticDefaults(texts().in(language));
    }

    public static Localized<CosmeticDefaults> localized() {
        return texts().map(CosmeticDefaults::new);
    }

    public static LocalizedConfig texts() {
        YamlConfiguration standard = Objects.requireNonNull(read(RESOURCE), "Missing " + RESOURCE);
        Map<Language, ConfigurationSection> translations = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            YamlConfiguration translation = language == Language.DEFAULT ? null : read(translation(language));
            if (translation != null) {
                translations.put(language, translation);
            }
        }
        return LocalizedConfig.of(standard, translations);
    }

    public static String translation(Language language) {
        return TRANSLATIONS + language.tag() + ".yml";
    }

    private static YamlConfiguration read(String resource) {
        InputStream found = CosmeticDefaults.class.getResourceAsStream(resource);
        if (found == null) {
            return null;
        }
        try (InputStream stream = found; Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static CosmeticDefaults of(ConfigurationSection defaults) {
        return new CosmeticDefaults(defaults);
    }

    public ConfigurationSection section(CosmeticKind kind, ConfigurationSection gameCosmetics) {
        ConfigurationSection override = gameCosmetics == null ? null : gameCosmetics.getConfigurationSection(kind.key());
        MemoryConfiguration merged = merge(defaults.getConfigurationSection(kind.key()), override);
        merged.set("category", null);
        ConfigurationSection list = merged.getConfigurationSection(LIST);
        if (list != null) {
            for (String id : list.getKeys(false)) {
                if (list.getBoolean(id + "." + HIDDEN, false)) {
                    list.set(id, null);
                }
            }
        }
        return merged;
    }

    public CosmeticCatalog catalog(String game, CosmeticKind kind, ConfigurationSection gameCosmetics, Predicate<String> animated) {
        return CosmeticCatalog.from(section(kind, gameCosmetics), LIST, kind.category(game), animated);
    }

    public ConfigurationSection killMessages(ConfigurationSection gameKillMessages) {
        return merge(defaults.getConfigurationSection(KILL_MESSAGES), gameKillMessages);
    }

    private static MemoryConfiguration merge(ConfigurationSection base, ConfigurationSection override) {
        MemoryConfiguration merged = new MemoryConfiguration();
        copy(base, merged);
        copy(override, merged);
        return merged;
    }

    private static void copy(ConfigurationSection from, MemoryConfiguration into) {
        if (from == null) {
            return;
        }
        for (String key : from.getKeys(true)) {
            if (!from.isConfigurationSection(key)) {
                into.set(key, from.get(key));
            }
        }
    }
}
