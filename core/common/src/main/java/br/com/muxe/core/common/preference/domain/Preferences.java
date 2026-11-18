package br.com.muxe.core.common.preference.domain;

import java.util.EnumMap;
import java.util.Map;

public record Preferences(Map<Preference, String> values) {

    public static final Preferences DEFAULTS = new Preferences(Map.of());

    public Preferences {
        EnumMap<Preference, String> kept = new EnumMap<>(Preference.class);
        values.forEach((preference, value) -> {
            if (preference.accepts(value)) {
                kept.put(preference, value);
            }
        });
        values = Map.copyOf(kept);
    }

    public String value(Preference preference) {
        return values.getOrDefault(preference, preference.defaultValue());
    }

    public boolean on(Preference preference) {
        return Boolean.parseBoolean(value(preference));
    }

    public PrivateMessages privateMessages() {
        return PrivateMessages.parse(value(Preference.PRIVATE_MESSAGES)).orElse(PrivateMessages.PUBLIC);
    }

    public TagFormat tagFormat() {
        return TagFormat.parse(value(Preference.TAG_FORMAT)).orElse(TagFormat.DEFAULT);
    }

    public String next(Preference preference) {
        return switch (preference.kind()) {
            case TOGGLE -> String.valueOf(!on(preference));
            case PRIVATE_MESSAGES -> privateMessages().next().name();
            case TAG_FORMAT -> tagFormat().next().name();
        };
    }

    public Preferences with(Preference preference, String value) {
        EnumMap<Preference, String> changed = new EnumMap<>(Preference.class);
        changed.putAll(values);
        changed.put(preference, value);
        return new Preferences(changed);
    }
}
