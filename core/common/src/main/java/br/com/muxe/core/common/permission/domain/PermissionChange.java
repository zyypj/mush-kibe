package br.com.muxe.core.common.permission.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public sealed interface PermissionChange {

    String describe();

    static PermissionData applyAll(PermissionData data, List<PermissionChange> changes) {
        Map<String, Group> groups = new HashMap<>(data.groups());
        Map<String, Tag> tags = new HashMap<>(data.tags());
        Map<String, Track> tracks = new HashMap<>(data.tracks());
        for (PermissionChange change : changes) {
            switch (change) {
                case PutGroup put -> groups.put(put.group().id(), put.group());
                case DeleteGroup delete -> groups.remove(delete.id());
                case PutTag put -> tags.put(put.tag().id(), put.tag());
                case DeleteTag delete -> tags.remove(delete.id());
                case PutTrack put -> tracks.put(put.track().id(), put.track());
                case DeleteTrack delete -> tracks.remove(delete.id());
                case PutUser ignored -> {
                }
            }
        }
        return new PermissionData(groups, tags, tracks);
    }

    record PutGroup(Group group) implements PermissionChange {
        public PutGroup {
            Objects.requireNonNull(group, "group");
        }

        @Override
        public String describe() {
            return "saved group " + group.id() + " (" + group.nodes().size() + " nodes, weight " + group.weight() + ")";
        }
    }

    record DeleteGroup(String id) implements PermissionChange {
        public DeleteGroup {
            PermissionIds.require(id);
        }

        @Override
        public String describe() {
            return "deleted group " + id;
        }
    }

    record PutTag(Tag tag) implements PermissionChange {
        public PutTag {
            Objects.requireNonNull(tag, "tag");
        }

        @Override
        public String describe() {
            return "saved tag " + tag.id() + " (" + tag.display() + ")";
        }
    }

    record DeleteTag(String id) implements PermissionChange {
        public DeleteTag {
            PermissionIds.require(id);
        }

        @Override
        public String describe() {
            return "deleted tag " + id;
        }
    }

    record PutTrack(Track track) implements PermissionChange {
        public PutTrack {
            Objects.requireNonNull(track, "track");
        }

        @Override
        public String describe() {
            return "saved track " + track.id() + " " + track.groups();
        }
    }

    record DeleteTrack(String id) implements PermissionChange {
        public DeleteTrack {
            PermissionIds.require(id);
        }

        @Override
        public String describe() {
            return "deleted track " + id;
        }
    }

    record PutUser(PermissionUser user) implements PermissionChange {
        public PutUser {
            Objects.requireNonNull(user, "user");
        }

        @Override
        public String describe() {
            return "saved user " + user.id() + " (" + user.nodes().size() + " nodes)";
        }
    }
}
