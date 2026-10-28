package br.com.muxe.core.common.permission.domain;

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
}
