package br.com.muxe.core.common.permission.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record PermissionNode(String key, boolean value, Contexts contexts, Optional<Instant> expiresAt, Instant addedAt, Instant updatedAt) {

    public static final String GROUP_PREFIX = "group.";
    public static final String TAG_PREFIX = "muxe.tag.";
    public static final String WILDCARD = "*";

    private static final Pattern KEY = Pattern.compile("[a-z0-9_.*+-]{1,128}");

    public PermissionNode {
        key = Objects.requireNonNull(key, "key").trim().toLowerCase(Locale.ROOT);
        if (!KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Permissions use lowercase letters, digits and . _ - + *, up to 128 characters, got: " + key);
        }
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(addedAt, "addedAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static PermissionNode permanent(String key, boolean value, Instant at) {
        return new PermissionNode(key, value, Contexts.NONE, Optional.empty(), at, at);
    }

    public static PermissionNode group(String groupId, Optional<Instant> expiresAt, Instant at) {
        return new PermissionNode(GROUP_PREFIX + groupId, true, Contexts.NONE, expiresAt, at, at);
    }

    public boolean isGroup() {
        return key.startsWith(GROUP_PREFIX) && key.length() > GROUP_PREFIX.length();
    }

    public String groupId() {
        if (!isGroup()) {
            throw new IllegalStateException(key + " is not a group node");
        }
        return key.substring(GROUP_PREFIX.length());
    }

    public boolean isActive(Instant now) {
        return expiresAt.map(now::isBefore).orElse(true);
    }

    public boolean appliesIn(Contexts current, Instant now) {
        return isActive(now) && contexts.satisfiedBy(current);
    }

    public boolean sameSlot(PermissionNode other) {
        return key.equals(other.key) && contexts.equals(other.contexts);
    }

    public PermissionNode updatedAt(Instant at) {
        return new PermissionNode(key, value, contexts, expiresAt, addedAt, at);
    }
}
