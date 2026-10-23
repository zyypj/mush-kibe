package br.com.muxe.core.bukkit.text;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public final class MessageCatalog {

    private final Map<String, String> messages;

    private MessageCatalog(Map<String, String> messages) {
        this.messages = Map.copyOf(messages);
    }

    public static MessageCatalog from(ConfigurationSection section) {
        Map<String, String> messages = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(true)) {
                Object value = section.get(key);
                if (value instanceof String text) {
                    messages.put(key, ChatColor.translateAlternateColorCodes('&', text));
                }
            }
        }
        return new MessageCatalog(messages);
    }

    public static MessageCatalog of(Map<String, String> rawMessages) {
        Map<String, String> messages = new HashMap<>();
        rawMessages.forEach((key, text) -> messages.put(key, ChatColor.translateAlternateColorCodes('&', text)));
        return new MessageCatalog(messages);
    }

    public String get(String key, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders need name and value pairs");
        }
        String message = messages.get(key);
        if (message == null) {
            return ChatColor.RED + "Missing message: " + key;
        }
        for (int index = 0; index < placeholders.length; index += 2) {
            message = message.replace("{" + placeholders[index] + "}", placeholders[index + 1]);
        }
        return message;
    }

    public MessageCatalog merge(MessageCatalog other) {
        Map<String, String> merged = new HashMap<>(messages);
        merged.putAll(other.messages);
        return new MessageCatalog(merged);
    }
}
