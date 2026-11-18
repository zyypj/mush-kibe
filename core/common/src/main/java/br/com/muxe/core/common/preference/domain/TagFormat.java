package br.com.muxe.core.common.preference.domain;

import java.util.Arrays;
import java.util.Optional;

public enum TagFormat {
    DEFAULT,
    COLOR,
    BRACKETS_UPPER,
    BRACKETS,
    DEFAULT_WHITE,
    DEFAULT_GRAY;

    public TagFormat next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Optional<TagFormat> parse(String value) {
        return Arrays.stream(values()).filter(format -> format.name().equals(value)).findFirst();
    }
}
