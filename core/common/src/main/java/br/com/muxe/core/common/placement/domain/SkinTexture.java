package br.com.muxe.core.common.placement.domain;

import java.util.Objects;

public record SkinTexture(String value, String signature) {

    public SkinTexture {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(signature, "signature");
    }
}
