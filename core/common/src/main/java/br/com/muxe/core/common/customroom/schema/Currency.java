package br.com.muxe.core.common.customroom.schema;

import java.util.Objects;

public record Currency(String id, String one, String many, String color, Icon icon) {

    public Currency {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(one, "one");
        Objects.requireNonNull(many, "many");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(icon, "icon");
    }

    public String amount(int value) {
        return color + value + " " + (value == 1 ? one : many);
    }
}
