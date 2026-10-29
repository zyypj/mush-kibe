package br.com.muxe.core.common.permission.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Group(String id, String displayName, int weight, Optional<String> tag, List<PermissionNode> nodes) {

    public static final String DEFAULT = "membro";

    public Group {
        PermissionIds.require(id);
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank() || displayName.length() > 32) {
            throw new IllegalArgumentException("A group display name has 1 to 32 characters, got: " + displayName);
        }
        Objects.requireNonNull(tag, "tag");
        nodes = List.copyOf(nodes);
    }

    public List<String> parents() {
        return nodes.stream().filter(PermissionNode::isGroup).map(PermissionNode::groupId).distinct().toList();
    }

    public Group withNodes(List<PermissionNode> replaced) {
        return new Group(id, displayName, weight, tag, replaced);
    }

    public Group with(PermissionNode node) {
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
        return withNodes(next);
    }

    public Optional<Group> without(String key, Contexts contexts) {
        List<PermissionNode> next = nodes.stream().filter(node -> !(node.key().equals(key) && node.contexts().equals(contexts))).toList();
        return next.size() == nodes.size() ? Optional.empty() : Optional.of(withNodes(next));
    }
}
