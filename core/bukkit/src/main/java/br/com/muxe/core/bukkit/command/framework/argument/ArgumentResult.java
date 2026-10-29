package br.com.muxe.core.bukkit.command.framework.argument;

import java.util.List;
import java.util.Objects;

public sealed interface ArgumentResult<T> permits ArgumentResult.Parsed, ArgumentResult.Rejected {

    static <T> ArgumentResult<T> of(T value) {
        return new Parsed<>(value);
    }

    static <T> ArgumentResult<T> rejected(String messageKey, String... placeholders) {
        return new Rejected<>(messageKey, List.of(placeholders));
    }

    record Parsed<T>(T value) implements ArgumentResult<T> {
    }

    record Rejected<T>(String messageKey, List<String> placeholders) implements ArgumentResult<T> {

        public Rejected {
            Objects.requireNonNull(messageKey, "messageKey");
            placeholders = List.copyOf(placeholders);
            if (placeholders.size() % 2 != 0) {
                throw new IllegalArgumentException("Placeholders need name and value pairs");
            }
        }
    }
}
