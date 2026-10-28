package br.com.muxe.core.common.permission.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PermissionUser(UUID id, List<PermissionNode> nodes, Optional<String> selectedTag) {

    public PermissionUser {
        Objects.requireNonNull(id, "id");
        nodes = List.copyOf(nodes);
        Objects.requireNonNull(selectedTag, "selectedTag");
    }

    public static PermissionUser empty(UUID id) {
        return new PermissionUser(id, List.of(), Optional.empty());
    }

    public List<PermissionNode> groupNodes(Instant now) {
        return nodes.stream().filter(node -> node.isGroup() && node.value() && node.isActive(now)).toList();
    }

    public PermissionUser with(PermissionNode node) {
        List<PermissionNode> next = new ArrayList<>();
        Instant added = node.addedAt();
        for (PermissionNode current : nodes) {
            if (current.sameSlot(node)) {
                added = current.addedAt();
            } else {
                next.add(current);
            }
        }
        next.add(new PermissionNode(node.key(), node.value(), node.contexts(), node.expiresAt(), added, node.updatedAt()));
        return new PermissionUser(id, next, selectedTag);
    }

    public PermissionUser without(String key, Contexts contexts) {
        return new PermissionUser(id, nodes.stream().filter(node -> !(node.key().equals(key) && node.contexts().equals(contexts))).toList(), selectedTag);
    }

    public PermissionUser withoutExpired(Instant now) {
        return new PermissionUser(id, nodes.stream().filter(node -> node.isActive(now)).toList(), selectedTag);
    }
}
