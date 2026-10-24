package br.com.muxe.core.bukkit.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public final class MessageCatalog {

    public static final String BLANK_LINE = " ";

    private final Map<String, List<String>> messages;

    private MessageCatalog(Map<String, List<String>> messages) {
        this.messages = Map.copyOf(messages);
    }

    public static MessageCatalog from(ConfigurationSection section) {
        Map<String, Object> raw = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(true)) {
                Object value = section.get(key);
                if (value instanceof String || value instanceof List<?>) {
                    raw.put(key, value);
                }
            }
        }
        return of(raw);
    }

    public static MessageCatalog of(Map<String, ?> rawMessages) {
        Map<String, List<String>> messages = new HashMap<>();
        rawMessages.forEach((key, value) -> {
            if (value instanceof String text) {
                messages.put(key, List.of(translate(text)));
            } else if (value instanceof List<?> lines) {
                messages.put(key, lines.stream().map(String::valueOf).map(MessageCatalog::translate).toList());
            }
        });
        return new MessageCatalog(messages);
    }

    public String get(String key, String... placeholders) {
        return String.join("\n", resolve(key, placeholders));
    }

    public List<String> lines(String key, String... placeholders) {
        return resolve(key, placeholders).stream().map(line -> line.isEmpty() ? BLANK_LINE : line).toList();
    }

    public MessageCatalog merge(MessageCatalog other) {
        Map<String, List<String>> merged = new HashMap<>(messages);
        merged.putAll(other.messages);
        return new MessageCatalog(merged);
    }

    private List<String> resolve(String key, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders need name and value pairs");
        }
        List<String> lines = messages.get(key);
        if (lines == null) {
            return List.of(ChatColor.RED + "Missing message: " + key);
        }
        List<String> resolved = new ArrayList<>(lines.size());
        for (String line : lines) {
            for (int index = 0; index < placeholders.length; index += 2) {
                line = line.replace("{" + placeholders[index] + "}", placeholders[index + 1]);
            }
            resolved.add(line);
        }
        return resolved;
    }

    private static String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
