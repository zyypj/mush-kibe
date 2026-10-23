package br.com.muxe.core.common.messaging.metrics;

import java.util.Locale;

public enum RequestOutcome {
    SUCCESS,
    TIMEOUT,
    FAILURE;

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }
}
