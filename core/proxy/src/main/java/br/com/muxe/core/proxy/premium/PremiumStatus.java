package br.com.muxe.core.proxy.premium;

import java.util.Locale;

public enum PremiumStatus {
    PREMIUM,
    CRACKED,
    UNKNOWN;

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }
}
