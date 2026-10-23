package br.com.muxe.core.common.result;

import java.util.Objects;

public record Problem(String code, String message) {

    public Problem {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (code.isBlank()) {
            throw new IllegalArgumentException("Problem code cannot be blank");
        }
    }

    public static Problem of(String code, String message) {
        return new Problem(code, message);
    }
}
