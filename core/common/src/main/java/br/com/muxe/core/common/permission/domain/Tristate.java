package br.com.muxe.core.common.permission.domain;

public enum Tristate {
    TRUE,
    FALSE,
    UNDEFINED;

    public static Tristate of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean asBoolean() {
        return this == TRUE;
    }
}
