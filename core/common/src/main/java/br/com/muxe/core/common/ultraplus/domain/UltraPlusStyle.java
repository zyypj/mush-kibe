package br.com.muxe.core.common.ultraplus.domain;

import java.util.Objects;
import java.util.Optional;

public record UltraPlusStyle(Optional<String> emblem, Optional<String> plusColor, int tagLevel) {

    public static final int FIRST_LEVEL = 1;
    public static final UltraPlusStyle DEFAULT = new UltraPlusStyle(Optional.empty(), Optional.empty(), FIRST_LEVEL);

    public UltraPlusStyle {
        Objects.requireNonNull(emblem, "emblem");
        Objects.requireNonNull(plusColor, "plusColor");
        if (tagLevel < FIRST_LEVEL) {
            throw new IllegalArgumentException("A tag level starts at 1, got: " + tagLevel);
        }
    }

    public UltraPlusStyle withEmblem(Optional<String> chosen) {
        return new UltraPlusStyle(chosen, plusColor, tagLevel);
    }

    public UltraPlusStyle withPlusColor(Optional<String> chosen) {
        return new UltraPlusStyle(emblem, chosen, tagLevel);
    }

    public UltraPlusStyle withTagLevel(int chosen) {
        return new UltraPlusStyle(emblem, plusColor, chosen);
    }
}
