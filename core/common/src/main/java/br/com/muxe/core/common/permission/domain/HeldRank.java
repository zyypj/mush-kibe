package br.com.muxe.core.common.permission.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record HeldRank(Group group, Optional<Tag> tag, PermissionNode node) {

    private static final String NO_TAG_COLOR = "&7";

    public HeldRank {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(node, "node");
    }

    public static List<HeldRank> of(PermissionData data, PermissionUser user, Instant now) {
        Map<String, HeldRank> held = new LinkedHashMap<>();
        user.groupNodes(now).stream()
            .filter(node -> !node.groupId().equals(Group.DEFAULT))
            .sorted(Comparator.comparing((PermissionNode node) -> node.expiresAt().isPresent()).thenComparing(node -> node.contexts().values().size()))
            .forEach(node -> data.group(node.groupId()).ifPresent(group ->
                held.putIfAbsent(group.id(), new HeldRank(group, group.tag().flatMap(data::tag), node))));
        return held.values().stream()
            .sorted(Comparator.comparingInt((HeldRank rank) -> rank.group().weight()).reversed().thenComparing(rank -> rank.group().id()))
            .toList();
    }

    public static Optional<HeldRank> defaultRank(PermissionData data, Instant at) {
        return data.group(Group.DEFAULT).map(group -> new HeldRank(group, group.tag().flatMap(data::tag), PermissionNode.group(group.id(), Optional.empty(), at)));
    }

    public String display() {
        return tag.map(Tag::display).orElse(NO_TAG_COLOR + group.displayName());
    }

    public String name() {
        return tag.map(Tag::name).orElse(group.displayName());
    }

    public boolean isPermanent() {
        return node.expiresAt().isEmpty();
    }

    public Optional<Duration> remaining(Instant now) {
        return node.expiresAt().map(at -> Duration.between(now, at));
    }
}
