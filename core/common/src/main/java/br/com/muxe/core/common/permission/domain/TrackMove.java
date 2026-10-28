package br.com.muxe.core.common.permission.domain;

import br.com.muxe.core.common.result.Result;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record TrackMove(Optional<String> removed, Optional<String> added) {

    public static final String TOP = "permission.track-top";
    public static final String BOTTOM = "permission.track-bottom";

    public TrackMove {
        Objects.requireNonNull(removed, "removed");
        Objects.requireNonNull(added, "added");
    }

    public static Result<TrackMove> promote(Track track, PermissionUser user, Instant now) {
        int current = position(track, user, now);
        if (current + 1 >= track.groups().size()) {
            return Result.failure(TOP, "The user is already at the top of " + track.id());
        }
        return Result.success(new TrackMove(held(track, current), Optional.of(track.groups().get(current + 1))));
    }

    public static Result<TrackMove> demote(Track track, PermissionUser user, Instant now) {
        int current = position(track, user, now);
        Optional<String> removed = held(track, current);
        if (removed.isEmpty()) {
            return Result.failure(BOTTOM, "The user is already at the bottom of " + track.id());
        }
        Optional<String> added = current > 0 ? Optional.of(track.groups().get(current - 1)) : Optional.empty();
        return Result.success(new TrackMove(removed, added.filter(group -> !group.equals(Group.DEFAULT))));
    }

    private static int position(Track track, PermissionUser user, Instant now) {
        Set<String> direct = user.groupNodes(now).stream()
            .filter(node -> node.contexts().isEmpty())
            .map(PermissionNode::groupId)
            .collect(Collectors.toSet());
        for (int index = track.groups().size() - 1; index >= 0; index--) {
            if (direct.contains(track.groups().get(index))) {
                return index;
            }
        }
        return track.groups().indexOf(Group.DEFAULT);
    }

    private static Optional<String> held(Track track, int index) {
        if (index < 0) {
            return Optional.empty();
        }
        String group = track.groups().get(index);
        return group.equals(Group.DEFAULT) ? Optional.empty() : Optional.of(group);
    }
}
