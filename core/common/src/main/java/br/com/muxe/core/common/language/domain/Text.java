package br.com.muxe.core.common.language.domain;

import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface Text {

    String in(Language language);

    static Text plain(String text) {
        Objects.requireNonNull(text, "text");
        return language -> text;
    }

    static Text of(String portuguese, String english) {
        Objects.requireNonNull(portuguese, "portuguese");
        Objects.requireNonNull(english, "english");
        return language -> language == Language.ENGLISH ? english : portuguese;
    }

    static Text of(Map<Language, String> texts) {
        Map<Language, String> copy = Map.copyOf(texts);
        String fallback = Objects.requireNonNull(copy.get(Language.DEFAULT), "texts need the default language");
        return language -> copy.getOrDefault(language, fallback);
    }

    static String render(Object value, Language language) {
        return value instanceof Text text ? text.in(language) : String.valueOf(value);
    }
}
