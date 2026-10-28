package br.com.muxe.core.common.permission.domain;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PermissionResolver {

    private PermissionResolver() {
    }

    public static ResolvedPermissions resolve(PermissionData data, PermissionUser user, Contexts contexts, Instant now) {
        List<PermissionNode> userNodes = applicable(user.nodes(), contexts, now);
        Set<String> denied = new HashSet<>();
        userNodes.stream().filter(node -> node.isGroup() && !node.value()).forEach(node -> denied.add(node.groupId()));

        Set<String> found = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        if (!denied.contains(Group.DEFAULT)) {
            pending.add(Group.DEFAULT);
        }
        userNodes.stream().filter(node -> node.isGroup() && node.value()).forEach(node -> pending.add(node.groupId()));
        while (!pending.isEmpty()) {
            String id = pending.poll();
            Optional<Group> group = data.group(id);
            if (denied.contains(id) || group.isEmpty() || !found.add(id)) {
                continue;
            }
            applicable(group.get().nodes(), contexts, now).stream()
                .filter(node -> node.isGroup() && node.value())
                .forEach(node -> pending.add(node.groupId()));
        }

        List<Group> groups = found.stream().map(id -> data.group(id).orElseThrow())
            .sorted(Comparator.comparingInt(Group::weight).reversed().thenComparing(Group::id))
            .toList();

        Map<String, Boolean> permissions = new HashMap<>();
        List<Instant> expiries = new ArrayList<>();
        List<Group> lowestFirst = new ArrayList<>(groups);
        Collections.reverse(lowestFirst);
        for (Group group : lowestFirst) {
            permissions.put(PermissionNode.GROUP_PREFIX + group.id(), true);
            group.tag().flatMap(data::tag).ifPresent(tag -> permissions.put(tag.permission(), true));
            apply(permissions, expiries, applicable(group.nodes(), contexts, now));
        }
        apply(permissions, expiries, userNodes);

        ResolvedPermissions partial = new ResolvedPermissions(permissions, groups, List.of(), Optional.empty());
        List<Tag> tags = data.tagsByWeight().stream().filter(tag -> partial.has(tag.permission())).toList();
        List<Medal> medals = data.medalsByWeight().stream().filter(medal -> partial.has(medal.permission())).toList();
        return new ResolvedPermissions(permissions, groups, tags, medals, expiries.stream().min(Comparator.naturalOrder()));
    }

    private static void apply(Map<String, Boolean> permissions, List<Instant> expiries, List<PermissionNode> nodes) {
        nodes.stream()
            .sorted(Comparator.comparingInt((PermissionNode node) -> node.contexts().values().size()))
            .forEach(node -> {
                permissions.put(node.key(), node.value());
                node.expiresAt().ifPresent(expiries::add);
            });
    }

    private static List<PermissionNode> applicable(List<PermissionNode> nodes, Contexts contexts, Instant now) {
        return nodes.stream().filter(node -> node.appliesIn(contexts, now)).toList();
    }
}
