package br.com.muxe.core.bukkit.text;

import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageLookup;
import br.com.muxe.core.common.language.domain.Text;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class MessageCatalog {

    public static final String BLANK_LINE = " ";

    private final Map<Language, Map<String, List<String>>> messages;
    private final LanguageLookup lookup;

    private MessageCatalog(Map<Language, Map<String, List<String>>> messages, LanguageLookup lookup) {
        Map<Language, Map<String, List<String>>> copy = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            copy.put(language, Map.copyOf(messages.getOrDefault(language, Map.of())));
        }
        this.messages = copy;
        this.lookup = lookup;
    }

    public static MessageCatalog from(ConfigurationSection section) {
        return new MessageCatalog(Map.of(Language.DEFAULT, read(section)), LanguageLookup.DEFAULT);
    }

    public static MessageCatalog from(LocalizedConfig config, String path) {
        Map<Language, Map<String, List<String>>> messages = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            messages.put(language, read(config.in(language).getConfigurationSection(path)));
        }
        return new MessageCatalog(messages, LanguageLookup.DEFAULT);
    }

    public static MessageCatalog of(Map<String, ?> rawMessages) {
        return new MessageCatalog(Map.of(Language.DEFAULT, parse(rawMessages)), LanguageLookup.DEFAULT);
    }

    public static MessageCatalog of(Map<Language, ? extends Map<String, ?>> rawMessages, LanguageLookup lookup) {
        Map<Language, Map<String, List<String>>> messages = new EnumMap<>(Language.class);
        rawMessages.forEach((language, raw) -> messages.put(language, parse(raw)));
        return new MessageCatalog(messages, lookup);
    }

    public MessageCatalog speaking(LanguageLookup lookup) {
        return new MessageCatalog(messages, Objects.requireNonNull(lookup, "lookup"));
    }

    public Language language(CommandSender viewer) {
        return viewer instanceof Player player ? lookup.of(player.getUniqueId()) : Language.DEFAULT;
    }

    public Language language(UUID viewer) {
        return lookup.of(viewer);
    }

    public LanguageLookup lookup() {
        return lookup;
    }

    public String get(String key, Object... placeholders) {
        return get(Language.DEFAULT, key, placeholders);
    }

    public String get(CommandSender viewer, String key, Object... placeholders) {
        return get(language(viewer), key, placeholders);
    }

    public String get(UUID viewer, String key, Object... placeholders) {
        return get(lookup.of(viewer), key, placeholders);
    }

    public String get(Language language, String key, Object... placeholders) {
        return String.join("\n", resolve(language, key, placeholders));
    }

    public List<String> lines(String key, Object... placeholders) {
        return lines(Language.DEFAULT, key, placeholders);
    }

    public List<String> lines(CommandSender viewer, String key, Object... placeholders) {
        return lines(language(viewer), key, placeholders);
    }

    public List<String> lines(UUID viewer, String key, Object... placeholders) {
        return lines(lookup.of(viewer), key, placeholders);
    }

    public List<String> lines(Language language, String key, Object... placeholders) {
        return resolve(language, key, placeholders).stream().map(line -> line.isEmpty() ? BLANK_LINE : line).toList();
    }

    public Text text(String key, Object... placeholders) {
        Object[] copy = placeholders.clone();
        return language -> get(language, key, copy);
    }

    public boolean has(String key) {
        return messages.get(Language.DEFAULT).containsKey(key);
    }

    public MessageCatalog merge(MessageCatalog other) {
        Map<Language, Map<String, List<String>>> merged = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            Map<String, List<String>> combined = new HashMap<>(messages.get(language));
            combined.putAll(other.messages.get(language));
            merged.put(language, combined);
        }
        return new MessageCatalog(merged, other.lookup != LanguageLookup.DEFAULT ? other.lookup : lookup);
    }

    private List<String> resolve(Language language, String key, Object... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders need name and value pairs");
        }
        List<String> lines = messages.get(language).get(key);
        if (lines == null) {
            lines = messages.get(Language.DEFAULT).get(key);
        }
        if (lines == null) {
            return List.of(ChatColor.RED + "Missing message: " + key);
        }
        if (placeholders.length == 0) {
            return lines;
        }
        String[] rendered = new String[placeholders.length];
        for (int index = 0; index < placeholders.length; index += 2) {
            rendered[index] = "{" + placeholders[index] + "}";
            rendered[index + 1] = Text.render(placeholders[index + 1], language);
        }
        List<String> resolved = new ArrayList<>(lines.size());
        for (String line : lines) {
            for (int index = 0; index < rendered.length; index += 2) {
                line = line.replace(rendered[index], rendered[index + 1]);
            }
            resolved.add(line);
        }
        return resolved;
    }

    private static Map<String, List<String>> read(ConfigurationSection section) {
        Map<String, Object> raw = new HashMap<>();
        if (section != null) {
            Set<String> keys = new LinkedHashSet<>(section.getKeys(true));
            ConfigurationSection defaults = section.getDefaultSection();
            if (defaults != null) {
                keys.addAll(defaults.getKeys(true));
            }
            for (String key : keys) {
                Object value = section.get(key);
                if (value instanceof String || value instanceof List<?>) {
                    raw.put(key, value);
                }
            }
        }
        return parse(raw);
    }

    private static Map<String, List<String>> parse(Map<String, ?> rawMessages) {
        Map<String, List<String>> messages = new HashMap<>();
        rawMessages.forEach((key, value) -> {
            if (value instanceof String text) {
                messages.put(key, List.of(translate(text)));
            } else if (value instanceof List<?> lines) {
                messages.put(key, lines.stream().map(String::valueOf).map(MessageCatalog::translate).toList());
            }
        });
        return messages;
    }

    private static String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
