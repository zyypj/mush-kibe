package br.com.muxe.core.common.placement.domain;

import java.util.Locale;
import java.util.Objects;

public record HeldItem(String material, int data) {

    public HeldItem {
        Objects.requireNonNull(material, "material");
        if (material.isBlank()) {
            throw new IllegalArgumentException("An item in hand needs a material");
        }
        if (data < 0 || data > Short.MAX_VALUE) {
            throw new IllegalArgumentException("The item data goes from 0 to " + Short.MAX_VALUE + ", got " + data);
        }
        material = material.trim().toUpperCase(Locale.ROOT);
    }
}
