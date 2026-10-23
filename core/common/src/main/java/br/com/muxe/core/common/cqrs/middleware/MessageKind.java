package br.com.muxe.core.common.cqrs.middleware;

import java.util.Locale;

public enum MessageKind {
    COMMAND,
    QUERY;

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }
}
