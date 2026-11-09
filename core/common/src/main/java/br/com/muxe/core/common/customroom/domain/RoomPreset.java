package br.com.muxe.core.common.customroom.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RoomPreset(UUID id, String code, UUID owner, String game, String name, String mode, List<String> maps, RoomOptions options, Instant createdAt) {

    public static final int MAX_NAME_LENGTH = 32;

    public RoomPreset {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(createdAt, "createdAt");
        maps = List.copyOf(maps);
    }

    public String mapsText() {
        return String.join(",", maps);
    }

    public static List<String> maps(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split(","));
    }
}
