package br.com.muxe.core.common.metrics;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record Tags(SortedMap<String, String> values) {

    private static final Pattern KEY = Pattern.compile("[a-z_][a-z0-9_]*");

    public static final Tags EMPTY = new Tags(new TreeMap<>());

    public Tags {
        Objects.requireNonNull(values, "values");
        values.forEach(Tags::validate);
        values = Collections.unmodifiableSortedMap(new TreeMap<>(values));
    }

    public static Tags of(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Tags need key and value pairs");
        }
        TreeMap<String, String> values = new TreeMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            values.put(keyValues[index], keyValues[index + 1]);
        }
        return new Tags(values);
    }

    public Tags and(String key, String value) {
        TreeMap<String, String> merged = new TreeMap<>(values);
        merged.put(key, value);
        return new Tags(merged);
    }

    public Tags and(Tags other) {
        TreeMap<String, String> merged = new TreeMap<>(values);
        merged.putAll(other.values);
        return new Tags(merged);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    private static void validate(String key, String value) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid tag key: " + key);
        }
        Objects.requireNonNull(value, "Tag value of " + key);
    }
}
