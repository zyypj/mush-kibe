package br.com.muxe.core.bukkit.npc;

import java.util.Objects;

public record NpcSkin(String value, String signature) {

    public NpcSkin {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(signature, "signature");
    }
}
