package br.com.muxe.core.common.config.source;

import br.com.muxe.core.common.config.ConfigException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface ConfigSource {

    Optional<Object> find(String path);

    default String string(String path) {
        return find(path).map(String::valueOf).orElseThrow(() -> ConfigException.missing(path));
    }

    default String string(String path, String fallback) {
        return find(path).map(String::valueOf).orElse(fallback);
    }

    default int integer(String path, int fallback) {
        Optional<Object> value = find(path);
        if (value.isEmpty()) {
            return fallback;
        }
        Object raw = value.get();
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException exception) {
            throw ConfigException.invalid(path, raw, "an integer");
        }
    }

    default boolean bool(String path, boolean fallback) {
        Optional<Object> value = find(path);
        if (value.isEmpty()) {
            return fallback;
        }
        Object raw = value.get();
        if (raw instanceof Boolean flag) {
            return flag;
        }
        return switch (String.valueOf(raw).trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on" -> true;
            case "false", "no", "off" -> false;
            default -> throw ConfigException.invalid(path, raw, "true or false");
        };
    }

    default Duration duration(String path, Duration fallback) {
        Optional<Object> value = find(path);
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return DurationParser.parse(String.valueOf(value.get()));
        } catch (IllegalArgumentException exception) {
            throw ConfigException.invalid(path, value.get(), "a duration such as 5s");
        }
    }

    default List<String> stringList(String path) {
        Optional<Object> value = find(path);
        if (value.isEmpty()) {
            return List.of();
        }
        if (value.get() instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        throw ConfigException.invalid(path, value.get(), "a list");
    }
}
