package br.com.muxe.core.common.preference.domain;

import java.util.Arrays;
import java.util.Optional;

public enum PrivateMessages {
    PUBLIC,
    LIMITED,
    FRIENDS,
    OFF;

    public PrivateMessages next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Optional<PrivateMessages> parse(String value) {
        return Arrays.stream(values()).filter(option -> option.name().equals(value)).findFirst();
    }
}
