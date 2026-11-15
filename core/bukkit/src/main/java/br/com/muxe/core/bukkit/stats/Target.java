package br.com.muxe.core.bukkit.stats;

import java.util.Objects;
import java.util.UUID;

public record Target(UUID id, String name, boolean self) {

    public Target {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
