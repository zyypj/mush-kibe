package br.com.muxe.core.bukkit.npc;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;

public record Npc(String id, Location location, Optional<NpcSkin> skin) {

    private static final String PROFILE_PREFIX = "npc-";
    private static final int PROFILE_HASH_LENGTH = 8;

    public Npc {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(skin, "skin");
        location = location.clone();
    }

    public String profileName() {
        return profileName(id);
    }

    public UUID profileId() {
        return profileId(id);
    }

    public static String profileName(String id) {
        String hash = Integer.toHexString(id.toLowerCase(Locale.ROOT).hashCode());
        String padded = "0".repeat(Math.max(0, PROFILE_HASH_LENGTH - hash.length())) + hash;
        return PROFILE_PREFIX + padded.substring(padded.length() - PROFILE_HASH_LENGTH);
    }

    public static UUID profileId(String id) {
        return UUID.nameUUIDFromBytes(("muxe-npc:" + id.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }
}
