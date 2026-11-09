package br.com.muxe.core.common.customroom.domain;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record RoomOptions(Map<String, String> values) {

    public static final RoomOptions EMPTY = new RoomOptions(Map.of());

    public RoomOptions {
        values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public int integer(String key, int fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public double decimal(String key, double fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value.trim().replace(',', '.'));
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public boolean flag(String key, boolean fallback) {
        String value = values.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public RoomOptions with(String key, String value) {
        Map<String, String> changed = new TreeMap<>(values);
        changed.put(key, value);
        return new RoomOptions(changed);
    }

    public RoomOptions without(String key) {
        if (!values.containsKey(key)) {
            return this;
        }
        Map<String, String> changed = new TreeMap<>(values);
        changed.remove(key);
        return new RoomOptions(changed);
    }

    public RoomOptions withoutPrefix(String prefix) {
        Map<String, String> changed = new TreeMap<>(values);
        changed.keySet().removeIf(key -> key.startsWith(prefix));
        return new RoomOptions(changed);
    }

    public Map<String, String> startingWith(String prefix) {
        Map<String, String> found = new TreeMap<>();
        values.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                found.put(key.substring(prefix.length()), value);
            }
        });
        return found;
    }

    public String encode() {
        StringBuilder text = new StringBuilder();
        values.forEach((key, value) -> text.append(escape(key)).append('=').append(escape(value)).append('\n'));
        return text.toString();
    }

    public static RoomOptions decode(String text) {
        Map<String, String> values = new TreeMap<>();
        for (String line : text.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            int split = separator(line);
            if (split < 0) {
                continue;
            }
            values.put(unescape(line.substring(0, split)), unescape(line.substring(split + 1)));
        }
        return new RoomOptions(values);
    }

    public static void write(PacketWriter out, RoomOptions options) {
        out.writeList(List.copyOf(options.values().entrySet()), (writer, entry) -> writer.writeString(entry.getKey()).writeString(entry.getValue()));
    }

    public static RoomOptions read(PacketReader in) {
        Map<String, String> values = new TreeMap<>();
        in.readList(reader -> Map.entry(reader.readString(), reader.readString())).forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        return new RoomOptions(values);
    }

    private static int separator(String line) {
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '\\') {
                index++;
            } else if (current == '=') {
                return index;
            }
        }
        return -1;
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=");
    }

    private static String unescape(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '\\' && index + 1 < text.length()) {
                char next = text.charAt(++index);
                result.append(next == 'n' ? '\n' : next);
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}
