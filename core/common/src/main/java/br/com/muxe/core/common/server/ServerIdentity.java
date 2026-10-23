package br.com.muxe.core.common.server;

import java.util.Objects;
import java.util.regex.Pattern;

public record ServerIdentity(String id, String group, ServerKind kind) {

    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");

    public ServerIdentity {
        Objects.requireNonNull(kind, "kind");
        requireName(id, "id");
        requireName(group, "group");
    }

    public boolean isProxy() {
        return kind == ServerKind.PROXY;
    }

    private static void requireName(String value, String field) {
        if (value == null || !NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("Server " + field + " must match " + NAME.pattern() + ", got: " + value);
        }
    }
}
