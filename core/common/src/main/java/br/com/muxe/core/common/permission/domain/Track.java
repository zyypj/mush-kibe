package br.com.muxe.core.common.permission.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public record Track(String id, List<String> groups) {

    public Track {
        PermissionIds.require(id);
        groups = List.copyOf(groups);
        groups.forEach(PermissionIds::require);
        if (new HashSet<>(groups).size() != groups.size()) {
            throw new IllegalArgumentException("A track lists each group once: " + groups);
        }
    }

    public Optional<String> next(String group) {
        int index = groups.indexOf(group);
        return index >= 0 && index + 1 < groups.size() ? Optional.of(groups.get(index + 1)) : Optional.empty();
    }

    public Optional<String> previous(String group) {
        int index = groups.indexOf(group);
        return index > 0 ? Optional.of(groups.get(index - 1)) : Optional.empty();
    }
}
