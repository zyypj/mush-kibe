package br.com.muxe.core.common.combat.knockback;

import java.util.Locale;
import java.util.Optional;

public enum ClickLimiter {
    OFF,
    LOW,
    NORMAL,
    HIGH;

    public ClickLimiter next() {
        ClickLimiter[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<ClickLimiter> parse(String text) {
        for (ClickLimiter limiter : values()) {
            if (limiter.key().equalsIgnoreCase(text.trim())) {
                return Optional.of(limiter);
            }
        }
        return Optional.empty();
    }
}
