package br.com.muxe.core.bukkit.display;

import java.util.Objects;

public record DisplayLabel(String text, double height) {

    public DisplayLabel {
        Objects.requireNonNull(text, "text");
    }
}
