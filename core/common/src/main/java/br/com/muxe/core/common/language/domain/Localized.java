package br.com.muxe.core.common.language.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class Localized<T> {

    private final Map<Language, T> values;

    private Localized(Map<Language, T> values) {
        this.values = values;
    }

    public static <T> Localized<T> of(Function<Language, T> factory) {
        Map<Language, T> values = new EnumMap<>(Language.class);
        for (Language language : Language.values()) {
            values.put(language, Objects.requireNonNull(factory.apply(language), "a localized value for " + language));
        }
        return new Localized<>(values);
    }

    public static <T> Localized<T> same(T value) {
        Objects.requireNonNull(value, "value");
        return of(language -> value);
    }

    public T in(Language language) {
        return values.get(language);
    }

    public T standard() {
        return values.get(Language.DEFAULT);
    }

    public <R> Localized<R> map(Function<T, R> mapper) {
        return of(language -> mapper.apply(values.get(language)));
    }
}
