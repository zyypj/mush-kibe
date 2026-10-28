package br.com.muxe.core.common.permission.domain;

import br.com.muxe.core.common.result.Problem;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PermissionRules {

    private PermissionRules() {
    }

    public static Optional<Problem> check(PermissionData data) {
        if (data.group(Group.DEFAULT).isEmpty()) {
            return problem(PermissionProblems.DEFAULT_GROUP_REQUIRED, "The group '" + Group.DEFAULT + "' cannot be removed");
        }
        for (Group group : data.groups().values()) {
            if (group.tag().isPresent() && data.tag(group.tag().get()).isEmpty()) {
                return problem(PermissionProblems.UNKNOWN_TAG, "Group '" + group.id() + "' uses the unknown tag '" + group.tag().get() + "'");
            }
            for (String parent : group.parents()) {
                if (data.group(parent).isEmpty()) {
                    return problem(PermissionProblems.UNKNOWN_GROUP, "Group '" + group.id() + "' inherits the unknown group '" + parent + "'");
                }
            }
            if (reaches(data, group.id(), group.id(), new HashSet<>())) {
                return problem(PermissionProblems.INHERITANCE_CYCLE, "Group '" + group.id() + "' ends up inheriting itself");
            }
        }
        for (Track track : data.tracks().values()) {
            for (String group : track.groups()) {
                if (data.group(group).isEmpty()) {
                    return problem(PermissionProblems.UNKNOWN_GROUP, "Track '" + track.id() + "' lists the unknown group '" + group + "'");
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Problem> checkUser(PermissionData data, PermissionUser user) {
        for (PermissionNode node : user.nodes()) {
            if (node.isGroup() && data.group(node.groupId()).isEmpty()) {
                return problem(PermissionProblems.UNKNOWN_GROUP, "The group '" + node.groupId() + "' does not exist");
            }
        }
        if (user.selectedMedal().isPresent() && data.medal(user.selectedMedal().get()).isEmpty()) {
            return problem(PermissionProblems.UNKNOWN_MEDAL, "The medal '" + user.selectedMedal().get() + "' does not exist");
        }
        if (user.selectedTag().isPresent() && data.tag(user.selectedTag().get()).isEmpty()) {
            return problem(PermissionProblems.UNKNOWN_TAG, "The tag '" + user.selectedTag().get() + "' does not exist");
        }
        return Optional.empty();
    }

    private static boolean reaches(PermissionData data, String from, String target, Set<String> seen) {
        if (!seen.add(from)) {
            return false;
        }
        List<String> parents = data.group(from).map(Group::parents).orElse(List.of());
        for (String parent : parents) {
            if (parent.equals(target) || reaches(data, parent, target, seen)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Problem> problem(String code, String message) {
        return Optional.of(new Problem(code, message));
    }
}
