package br.com.muxe.core.bukkit.language;

import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocalizedConfig {

    private final Configuration standard;
    private final Map<Language, ConfigurationSection> translations;
    private final Map<Language, ConfigurationSection> roots;

    private LocalizedConfig(Configuration standard, Map<Language, ConfigurationSection> translations) {
        this.standard = standard;
        this.translations = Map.copyOf(translations);
        Map<Language, ConfigurationSection> roots = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            roots.put(language, language == Language.DEFAULT ? standard : overlay(standard, translations.get(language)));
        }
        this.roots = roots;
    }

    public static LocalizedConfig of(Configuration standard, Map<Language, ? extends ConfigurationSection> translations) {
        Objects.requireNonNull(standard, "standard");
        Map<Language, ConfigurationSection> copy = new EnumMap<>(Language.class);
        translations.forEach((language, section) -> {
            if (language != Language.DEFAULT && section != null) {
                copy.put(language, section);
            }
        });
        return new LocalizedConfig(standard, copy);
    }

    public static LocalizedConfig standardOnly(Configuration standard) {
        return of(standard, Map.of());
    }

    public static LocalizedConfig load(JavaPlugin plugin) {
        Map<Language, ConfigurationSection> translations = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            if (language != Language.DEFAULT) {
                translations.put(language, translation(plugin, language));
            }
        }
        return of(plugin.getConfig(), translations);
    }

    public static String resource(Language language) {
        return "lang/" + language.tag() + ".yml";
    }

    public ConfigurationSection standard() {
        return standard;
    }

    public ConfigurationSection in(Language language) {
        return roots.get(language);
    }

    public <T> Localized<T> map(Function<ConfigurationSection, T> reader) {
        return Localized.of(language -> reader.apply(roots.get(language)));
    }

    public Set<String> untranslated(Language language, String path) {
        Set<String> missing = new LinkedHashSet<>();
        if (language == Language.DEFAULT) {
            return missing;
        }
        ConfigurationSection source = path.isEmpty() ? standard : standard.getConfigurationSection(path);
        if (source == null) {
            return missing;
        }
        ConfigurationSection translation = translations.get(language);
        String prefix = path.isEmpty() ? "" : path + ".";
        for (String key : source.getKeys(true)) {
            Object value = source.get(key);
            if (readable(value)) {
                if (translation == null || !translation.contains(prefix + key)) {
                    missing.add(prefix + key);
                }
            }
        }
        return missing;
    }

    public static LocalizedConfig bundled(Class<?> owner, String config) {
        YamlConfiguration standard = read(owner, config);
        if (standard == null) {
            throw new IllegalArgumentException("No bundled " + config);
        }
        Map<Language, ConfigurationSection> translations = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            YamlConfiguration translation = language == Language.DEFAULT ? null : read(owner, resource(language));
            if (translation != null) {
                translations.put(language, translation);
            }
        }
        return of(standard, translations);
    }

    private static boolean readable(Object value) {
        if (value instanceof String text) {
            return readable(text);
        }
        if (value instanceof List<?> lines) {
            return lines.stream().anyMatch(line -> line instanceof String text && readable(text));
        }
        return false;
    }

    private static boolean readable(String text) {
        if (text.indexOf('&') >= 0 || text.indexOf('§') >= 0) {
            return text.chars().anyMatch(Character::isLetter);
        }
        return text.indexOf(' ') >= 0 && text.chars().anyMatch(Character::isLowerCase);
    }

    private static YamlConfiguration read(Class<?> owner, String resource) {
        InputStream stream = owner.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read " + resource, exception);
        }
    }

    private static ConfigurationSection overlay(Configuration standard, ConfigurationSection translation) {
        MemoryConfiguration root = new MemoryConfiguration();
        root.setDefaults(standard);
        root.options().copyDefaults(true);
        copyLeaves(standard, root);
        if (translation != null) {
            copyLeaves(translation, root);
        }
        return root;
    }

    private static void copyLeaves(ConfigurationSection from, ConfigurationSection to) {
        Set<String> keys = new LinkedHashSet<>(from.getKeys(true));
        ConfigurationSection defaults = from.getDefaultSection();
        if (defaults != null) {
            keys.addAll(defaults.getKeys(true));
        }
        for (String key : keys) {
            Object value = from.get(key);
            if (value != null && !(value instanceof ConfigurationSection)) {
                to.set(key, value);
            }
        }
    }

    private static ConfigurationSection translation(JavaPlugin plugin, Language language) {
        String resource = resource(language);
        YamlConfiguration bundled = bundled(plugin, resource);
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists() && bundled != null) {
            plugin.saveResource(resource, false);
        }
        if (!file.exists()) {
            return bundled;
        }
        YamlConfiguration own = YamlConfiguration.loadConfiguration(file);
        if (bundled != null) {
            own.setDefaults(bundled);
        }
        return own;
    }

    private static YamlConfiguration bundled(JavaPlugin plugin, String resource) {
        return read(plugin.getClass(), resource);
    }
}
