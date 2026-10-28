package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.permission.domain.Track;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryPermissionRepository implements PermissionRepository {

    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final Map<String, Tag> tags = new ConcurrentHashMap<>();
    private final Map<String, Track> tracks = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionUser> users = new ConcurrentHashMap<>();
    private final List<PermissionAction> actions = new CopyOnWriteArrayList<>();

    public static InMemoryPermissionRepository seeded() {
        InMemoryPermissionRepository repository = new InMemoryPermissionRepository();
        PermissionFixtures.DATA.tags().values().forEach(repository::saveTag);
        PermissionFixtures.DATA.groups().values().forEach(repository::saveGroup);
        PermissionFixtures.DATA.tracks().values().forEach(repository::saveTrack);
        return repository;
    }

    @Override
    public PermissionData load() {
        return new PermissionData(groups, tags, tracks);
    }

    @Override
    public PermissionUser user(UUID id) {
        return users.getOrDefault(id, PermissionUser.empty(id));
    }

    @Override
    public void saveGroup(Group group) {
        groups.put(group.id(), group);
    }

    @Override
    public void deleteGroup(String id) {
        groups.remove(id);
    }

    @Override
    public void saveTag(Tag tag) {
        tags.put(tag.id(), tag);
    }

    @Override
    public void deleteTag(String id) {
        tags.remove(id);
        groups.replaceAll((key, group) -> group.tag().filter(id::equals).isPresent()
            ? new Group(group.id(), group.displayName(), group.weight(), Optional.empty(), group.nodes())
            : group);
        users.replaceAll((key, user) -> user.selectedTag().filter(id::equals).isPresent()
            ? new PermissionUser(user.id(), user.nodes(), Optional.empty())
            : user);
    }

    @Override
    public void saveTrack(Track track) {
        tracks.put(track.id(), track);
    }

    @Override
    public void deleteTrack(String id) {
        tracks.remove(id);
    }

    @Override
    public void saveUser(PermissionUser user) {
        users.put(user.id(), user);
    }

    @Override
    public void selectTag(UUID id, Optional<String> tag) {
        PermissionUser user = user(id);
        users.put(id, new PermissionUser(id, user.nodes(), tag));
    }

    @Override
    public List<UUID> usersWith(String key) {
        return users.values().stream().filter(user -> user.nodes().stream().anyMatch(node -> node.key().equals(key))).map(PermissionUser::id).sorted().toList();
    }

    @Override
    public void log(PermissionAction action) {
        actions.add(action);
    }

    @Override
    public List<PermissionAction> recentActions(int limit) {
        List<PermissionAction> recent = new ArrayList<>(actions);
        Collections.reverse(recent);
        return recent.stream().limit(limit).toList();
    }
}
