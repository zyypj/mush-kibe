package br.com.muxe.core.common.permission.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PermissionData(Map<String, Group> groups, Map<String, Tag> tags, Map<String, Track> tracks, Map<String, Medal> medals) {

    public static final PermissionData EMPTY = new PermissionData(Map.of(), Map.of(), Map.of(), Map.of());

    public PermissionData {
        groups = Map.copyOf(groups);
        tags = Map.copyOf(tags);
        tracks = Map.copyOf(tracks);
        medals = Map.copyOf(medals);
    }

    public PermissionData(Map<String, Group> groups, Map<String, Tag> tags, Map<String, Track> tracks) {
        this(groups, tags, tracks, Map.of());
    }

    public static PermissionData of(List<Group> groups, List<Tag> tags, List<Track> tracks) {
        return new PermissionData(
            groups.stream().collect(Collectors.toMap(Group::id, Function.identity())),
            tags.stream().collect(Collectors.toMap(Tag::id, Function.identity())),
            tracks.stream().collect(Collectors.toMap(Track::id, Function.identity()))
        );
    }

    public static PermissionData of(List<Group> groups, List<Tag> tags, List<Track> tracks, List<Medal> medals) {
        return new PermissionData(
            groups.stream().collect(Collectors.toMap(Group::id, Function.identity())),
            tags.stream().collect(Collectors.toMap(Tag::id, Function.identity())),
            tracks.stream().collect(Collectors.toMap(Track::id, Function.identity())),
            medals.stream().collect(Collectors.toMap(Medal::id, Function.identity()))
        );
    }

    public Optional<Medal> medal(String id) {
        return Optional.ofNullable(medals.get(id));
    }

    public List<Medal> medalsByWeight() {
        return medals.values().stream().sorted(Comparator.comparingInt(Medal::weight).thenComparing(Medal::id)).toList();
    }

    public Optional<Group> group(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    public Optional<Tag> tag(String id) {
        return Optional.ofNullable(tags.get(id));
    }

    public Optional<Track> track(String id) {
        return Optional.ofNullable(tracks.get(id));
    }

    public List<Group> groupsByWeight() {
        return groups.values().stream().sorted(Comparator.comparingInt(Group::weight).reversed().thenComparing(Group::id)).toList();
    }

    public List<Tag> tagsByWeight() {
        return tags.values().stream().sorted(Comparator.comparingInt(Tag::weight).reversed().thenComparing(Tag::id)).toList();
    }
}
