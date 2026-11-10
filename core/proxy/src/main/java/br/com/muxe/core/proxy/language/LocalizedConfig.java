package br.com.muxe.core.proxy.language;

import br.com.muxe.core.common.config.source.ConfigSource;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class LocalizedConfig {

    private final ConfigSource standard;
    private final Map<Language, ConfigSource> translations;
    private final Map<Language, ConfigSource> roots;

    private LocalizedConfig(ConfigSource standard, Map<Language, ConfigSource> translations) {
        this.standard = standard;
        this.translations = translations;
        Map<Language, ConfigSource> roots = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            ConfigSource translation = translations.get(language);
            roots.put(language, translation == null ? standard : layered(translation, standard));
        }
        this.roots = roots;
    }

    public static LocalizedConfig of(ConfigSource standard, Map<Language, ? extends ConfigSource> translations) {
        Objects.requireNonNull(standard, "standard");
        Map<Language, ConfigSource> copy = new EnumMap<>(Language.class);
        translations.forEach((language, source) -> {
            if (language != Language.DEFAULT && source != null) {
                copy.put(language, source);
            }
        });
        return new LocalizedConfig(standard, copy);
    }

    public static LocalizedConfig standardOnly(ConfigSource standard) {
        return of(standard, Map.of());
    }

    public static ConfigSource layered(ConfigSource preferred, ConfigSource fallback) {
        return path -> preferred.find(path).or(() -> fallback.find(path));
    }

    public static String resource(Language language) {
        return "lang/" + language.tag() + ".yml";
    }

    public ConfigSource standard() {
        return standard;
    }

    public ConfigSource in(Language language) {
        return roots.get(language);
    }

    public <T> Localized<T> map(Function<ConfigSource, T> reader) {
        return Localized.of(language -> reader.apply(roots.get(language)));
    }

    public Set<String> untranslated(Language language, String path) {
        Set<String> missing = new LinkedHashSet<>();
        if (language == Language.DEFAULT) {
            return missing;
        }
        ConfigSource translation = translations.get(language);
        standard.find(path).ifPresent(value -> collect(path, value, translation, missing));
        return missing;
    }

    private static void collect(String path, Object value, ConfigSource translation, Set<String> missing) {
        if (value instanceof Map<?, ?> section) {
            section.forEach((key, child) -> collect(path + "." + key, child, translation, missing));
            return;
        }
        if (readable(value) && (translation == null || translation.find(path).isEmpty())) {
            missing.add(path);
        }
    }

    private static boolean readable(Object value) {
        if (value instanceof String text) {
            return readable(text);
        }
        if (value instanceof Collection<?> lines) {
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
}
