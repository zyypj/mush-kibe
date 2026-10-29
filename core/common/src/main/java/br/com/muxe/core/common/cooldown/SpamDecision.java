package br.com.muxe.core.common.cooldown;

import java.time.Duration;

public record SpamDecision(boolean allowed, Duration remaining) {

    private static final SpamDecision ALLOWED = new SpamDecision(true, Duration.ZERO);

    public static SpamDecision allow() {
        return ALLOWED;
    }

    public static SpamDecision block(Duration remaining) {
        return new SpamDecision(false, remaining);
    }
}
