package br.com.muxe.core.common.ultraplus.domain;

import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record UltraPlusProfile(UUID playerId, int grantedDays, UltraPlusStyle style, Optional<YearMonth> rewardMonth) {

    public static final int DAYS_PER_MONTH = 30;

    public UltraPlusProfile {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(rewardMonth, "rewardMonth");
        if (grantedDays < 0) {
            throw new IllegalArgumentException("Granted days cannot be negative, got: " + grantedDays);
        }
    }

    public static UltraPlusProfile empty(UUID playerId) {
        return new UltraPlusProfile(playerId, 0, UltraPlusStyle.DEFAULT, Optional.empty());
    }

    public int months() {
        return grantedDays / DAYS_PER_MONTH;
    }

    public boolean claimed(YearMonth month) {
        return rewardMonth.filter(month::equals).isPresent();
    }

    public UltraPlusProfile withStyle(UltraPlusStyle chosen) {
        return new UltraPlusProfile(playerId, grantedDays, chosen, rewardMonth);
    }

    public UltraPlusProfile plusDays(int days) {
        return new UltraPlusProfile(playerId, grantedDays + Math.max(0, days), style, rewardMonth);
    }

    public UltraPlusProfile claim(YearMonth month) {
        return new UltraPlusProfile(playerId, grantedDays, style, Optional.of(month));
    }
}
