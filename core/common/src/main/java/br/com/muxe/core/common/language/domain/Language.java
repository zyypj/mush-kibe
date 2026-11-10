package br.com.muxe.core.common.language.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum Language {
    PORTUGUESE("pt-BR", "portugues", "portuguese", "pt", "br"),
    ENGLISH("en", "ingles", "english", "en-us");

    public static final Language DEFAULT = PORTUGUESE;

    private final String tag;
    private final List<String> names;

    Language(String tag, String... names) {
        this.tag = tag;
        this.names = List.of(names);
    }

    public String tag() {
        return tag;
    }

    public static Optional<Language> fromTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        for (Language language : values()) {
            if (language.tag.equalsIgnoreCase(tag) || language.name().equalsIgnoreCase(tag)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }

    public static Optional<Language> named(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String plain = Normalizer.normalize(name.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (Language language : values()) {
            if (language.tag.equalsIgnoreCase(plain) || language.name().equalsIgnoreCase(plain) || language.names.contains(plain)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }

    public List<String> names() {
        return names;
    }

    public static Language parse(String tag) {
        return fromTag(tag).orElse(DEFAULT);
    }
}
