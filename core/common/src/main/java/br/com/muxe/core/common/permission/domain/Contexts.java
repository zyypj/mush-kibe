package br.com.muxe.core.common.permission.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record Contexts(Map<String, String> values) {

    public static final Contexts NONE = new Contexts(Map.of());
    public static final String SERVER = "server";
    public static final String WORLD = "world";

    private static final String PAIR_SEPARATOR = ";";
    private static final String KEY_SEPARATOR = "=";

    public Contexts {
        TreeMap<String, String> sorted = new TreeMap<>();
        values.forEach((key, value) -> {
            String cleanKey = clean(key);
            String cleanValue = clean(value);
            if (cleanKey.isEmpty() || cleanValue.isEmpty() || cleanKey.contains(KEY_SEPARATOR) || cleanKey.contains(PAIR_SEPARATOR)
                || cleanValue.contains(KEY_SEPARATOR) || cleanValue.contains(PAIR_SEPARATOR)) {
                throw new IllegalArgumentException("Invalid context " + key + "=" + value);
            }
            sorted.put(cleanKey, cleanValue);
        });
        values = Map.copyOf(sorted);
    }

    public static Contexts of(String key, String value) {
        return new Contexts(Map.of(key, value));
    }

    public static Contexts server(String group, String world) {
        return new Contexts(Map.of(SERVER, group, WORLD, world));
    }

    public static Contexts parse(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return NONE;
        }
        TreeMap<String, String> values = new TreeMap<>();
        for (String pair : encoded.split(PAIR_SEPARATOR)) {
            int separator = pair.indexOf(KEY_SEPARATOR);
            if (separator <= 0) {
                throw new IllegalArgumentException("Invalid contexts: " + encoded);
            }
            values.put(pair.substring(0, separator), pair.substring(separator + 1));
        }
        return new Contexts(values);
    }

    public String encode() {
        return new TreeMap<>(values).entrySet().stream()
            .map(entry -> entry.getKey() + KEY_SEPARATOR + entry.getValue())
            .collect(Collectors.joining(PAIR_SEPARATOR));
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean satisfiedBy(Contexts current) {
        return values.entrySet().stream().allMatch(entry -> entry.getValue().equals(current.values().get(entry.getKey())));
    }

    private static String clean(String text) {
        return Objects.requireNonNull(text, "context").trim().toLowerCase(Locale.ROOT);
    }
}
