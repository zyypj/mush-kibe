package br.com.muxe.core.common.customroom.schema;

import java.util.Objects;

public record Icon(String material, int data, int amount, boolean glow, String texture) {

    public Icon {
        Objects.requireNonNull(material, "material");
        texture = texture == null ? "" : texture;
        amount = Math.max(1, Math.min(64, amount));
    }

    public static Icon of(String material) {
        return new Icon(material, 0, 1, false, "");
    }

    public static Icon of(String material, int data) {
        return new Icon(material, data, 1, false, "");
    }

    public Icon withAmount(int value) {
        return new Icon(material, data, value, glow, texture);
    }

    public Icon withGlow(boolean value) {
        return new Icon(material, data, amount, value, texture);
    }

    public Icon withMaterial(String value, int valueData) {
        return new Icon(value, valueData, amount, glow, texture);
    }
}
