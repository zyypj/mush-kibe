package br.com.muxe.core.common.permission.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record ResolvedPermissions(Map<String, Boolean> permissions, List<Group> groups, List<Tag> tags, Optional<Instant> nextExpiry) {

    public static final String META_PREFIX = "meta.";

    public ResolvedPermissions {
        permissions = Map.copyOf(permissions);
        groups = List.copyOf(groups);
        tags = List.copyOf(tags);
    }

    public Tristate check(String permission) {
        String key = permission.toLowerCase(Locale.ROOT);
        Boolean exact = permissions.get(key);
        if (exact != null) {
            return Tristate.of(exact);
        }
        String rest = key;
        int dot;
        while ((dot = rest.lastIndexOf('.')) > 0) {
            rest = rest.substring(0, dot);
            Boolean wildcard = permissions.get(rest + "." + PermissionNode.WILDCARD);
            if (wildcard != null) {
                return Tristate.of(wildcard);
            }
        }
        Boolean all = permissions.get(PermissionNode.WILDCARD);
        return all == null ? Tristate.UNDEFINED : Tristate.of(all);
    }

    public boolean has(String permission) {
        return check(permission).asBoolean();
    }

    public Optional<Group> primaryGroup() {
        return groups.stream().findFirst();
    }

    public boolean canUse(Tag tag) {
        return tags.contains(tag);
    }

    public Optional<Tag> displayTag(Optional<String> selected) {
        Optional<Tag> chosen = selected.flatMap(id -> tags.stream().filter(tag -> tag.id().equals(id)).findFirst());
        return chosen.isPresent() ? chosen : tags.stream().findFirst();
    }

    public Optional<String> meta(String key) {
        String prefix = META_PREFIX + key.toLowerCase(Locale.ROOT) + ".";
        return permissions.entrySet().stream()
            .filter(entry -> entry.getValue() && entry.getKey().startsWith(prefix))
            .map(entry -> entry.getKey().substring(prefix.length()))
            .sorted()
            .findFirst();
    }
}
