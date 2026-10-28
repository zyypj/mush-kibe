package br.com.muxe.core.common.permission.infrastructure.postgres;

import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionAction;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionRepository;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.permission.domain.Track;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PostgresPermissionRepository implements PermissionRepository {

    private static final String NODE_COLUMNS = "permission, value, contexts, expires_at, added_at, updated_at";

    private static final String FIND_TAGS = "SELECT id, name, color, weight, prefix, decorative FROM core.perm_tags";
    private static final String FIND_GROUPS = "SELECT id, display_name, weight, tag_id FROM core.perm_groups";
    private static final String FIND_GROUP_NODES = "SELECT group_id, " + NODE_COLUMNS + " FROM core.perm_group_nodes";
    private static final String FIND_TRACKS = "SELECT id, groups FROM core.perm_tracks";
    private static final String FIND_USER_NODES = "SELECT " + NODE_COLUMNS + " FROM core.perm_user_nodes WHERE player_id = ?";
    private static final String FIND_USER_TAG = "SELECT tag_id FROM core.player_tags WHERE player_id = ?";
    private static final String FIND_USERS_WITH = "SELECT DISTINCT player_id FROM core.perm_user_nodes WHERE permission = ? ORDER BY player_id";

    private static final String SAVE_TAG = """
        INSERT INTO core.perm_tags (id, name, color, weight, prefix, decorative) VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, color = EXCLUDED.color, weight = EXCLUDED.weight,
            prefix = EXCLUDED.prefix, decorative = EXCLUDED.decorative
        """;
    private static final String SAVE_GROUP = """
        INSERT INTO core.perm_groups (id, display_name, weight, tag_id) VALUES (?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET display_name = EXCLUDED.display_name, weight = EXCLUDED.weight, tag_id = EXCLUDED.tag_id
        """;
    private static final String SAVE_GROUP_NODE = "INSERT INTO core.perm_group_nodes (group_id, " + NODE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SAVE_USER_NODE = "INSERT INTO core.perm_user_nodes (player_id, " + NODE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SAVE_TRACK = "INSERT INTO core.perm_tracks (id, groups) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET groups = EXCLUDED.groups";
    private static final String SELECT_TAG = """
        INSERT INTO core.player_tags (player_id, tag_id, updated_at) VALUES (?, ?, now())
        ON CONFLICT (player_id) DO UPDATE SET tag_id = EXCLUDED.tag_id, updated_at = EXCLUDED.updated_at
        """;
    private static final String LOG = "INSERT INTO core.perm_actions (at, actor, description) VALUES (?, ?, ?)";
    private static final String RECENT = "SELECT at, actor, description FROM core.perm_actions ORDER BY id DESC LIMIT ?";

    @Override
    public PermissionData load() {
        try {
            Connection connection = TransactionContext.connection();
            List<Tag> tags = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(FIND_TAGS); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    tags.add(new Tag(rows.getString("id"), rows.getString("name"), rows.getString("color"), rows.getInt("weight"),
                        Optional.ofNullable(rows.getString("prefix")), rows.getBoolean("decorative")));
                }
            }
            Map<String, List<PermissionNode>> nodes = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(FIND_GROUP_NODES); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    nodes.computeIfAbsent(rows.getString("group_id"), ignored -> new ArrayList<>()).add(node(rows));
                }
            }
            List<Group> groups = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(FIND_GROUPS); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String id = rows.getString("id");
                    groups.add(new Group(id, rows.getString("display_name"), rows.getInt("weight"), Optional.ofNullable(rows.getString("tag_id")),
                        nodes.getOrDefault(id, List.of())));
                }
            }
            List<Track> tracks = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(FIND_TRACKS); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Array array = rows.getArray("groups");
                    tracks.add(new Track(rows.getString("id"), Arrays.asList((String[]) array.getArray())));
                }
            }
            return PermissionData.of(groups, tags, tracks);
        } catch (SQLException exception) {
            throw new PersistenceException("permission.load", exception);
        }
    }

    @Override
    public PermissionUser user(UUID id) {
        try {
            Connection connection = TransactionContext.connection();
            List<PermissionNode> nodes = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(FIND_USER_NODES)) {
                statement.setObject(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        nodes.add(node(rows));
                    }
                }
            }
            Optional<String> tag = Optional.empty();
            try (PreparedStatement statement = connection.prepareStatement(FIND_USER_TAG)) {
                statement.setObject(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next()) {
                        tag = Optional.ofNullable(rows.getString("tag_id"));
                    }
                }
            }
            return new PermissionUser(id, nodes, tag);
        } catch (SQLException exception) {
            throw new PersistenceException("permission.find_user", exception);
        }
    }

    @Override
    public void saveGroup(Group group) {
        try {
            Connection connection = TransactionContext.connection();
            try (PreparedStatement statement = connection.prepareStatement(SAVE_GROUP)) {
                statement.setString(1, group.id());
                statement.setString(2, group.displayName());
                statement.setInt(3, group.weight());
                statement.setString(4, group.tag().orElse(null));
                statement.executeUpdate();
            }
            delete(connection, "DELETE FROM core.perm_group_nodes WHERE group_id = ?", group.id());
            try (PreparedStatement statement = connection.prepareStatement(SAVE_GROUP_NODE)) {
                for (PermissionNode node : group.nodes()) {
                    statement.setString(1, group.id());
                    setNode(statement, 2, node);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("permission.save_group", exception);
        }
    }

    @Override
    public void deleteGroup(String id) {
        run("permission.delete_group", "DELETE FROM core.perm_groups WHERE id = ?", id);
    }

    @Override
    public void saveTag(Tag tag) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(SAVE_TAG)) {
            statement.setString(1, tag.id());
            statement.setString(2, tag.name());
            statement.setString(3, tag.color());
            statement.setInt(4, tag.weight());
            statement.setString(5, tag.prefix().orElse(null));
            statement.setBoolean(6, tag.decorative());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("permission.save_tag", exception);
        }
    }

    @Override
    public void deleteTag(String id) {
        run("permission.delete_tag", "DELETE FROM core.perm_tags WHERE id = ?", id);
    }

    @Override
    public void saveTrack(Track track) {
        try {
            Connection connection = TransactionContext.connection();
            try (PreparedStatement statement = connection.prepareStatement(SAVE_TRACK)) {
                statement.setString(1, track.id());
                statement.setArray(2, connection.createArrayOf("text", track.groups().toArray()));
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("permission.save_track", exception);
        }
    }

    @Override
    public void deleteTrack(String id) {
        run("permission.delete_track", "DELETE FROM core.perm_tracks WHERE id = ?", id);
    }

    @Override
    public void saveUser(PermissionUser user) {
        try {
            Connection connection = TransactionContext.connection();
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM core.perm_user_nodes WHERE player_id = ?")) {
                statement.setObject(1, user.id());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(SAVE_USER_NODE)) {
                for (PermissionNode node : user.nodes()) {
                    statement.setObject(1, user.id());
                    setNode(statement, 2, node);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            selectTag(user.id(), user.selectedTag());
        } catch (SQLException exception) {
            throw new PersistenceException("permission.save_user", exception);
        }
    }

    @Override
    public void selectTag(UUID id, Optional<String> tag) {
        try {
            Connection connection = TransactionContext.connection();
            if (tag.isEmpty()) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM core.player_tags WHERE player_id = ?")) {
                    statement.setObject(1, id);
                    statement.executeUpdate();
                }
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(SELECT_TAG)) {
                statement.setObject(1, id);
                statement.setString(2, tag.get());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("permission.select_tag", exception);
        }
    }

    @Override
    public List<UUID> usersWith(String key) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(FIND_USERS_WITH)) {
            statement.setString(1, key);
            List<UUID> users = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    users.add(rows.getObject("player_id", UUID.class));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw new PersistenceException("permission.find_users_with", exception);
        }
    }

    @Override
    public void log(PermissionAction action) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(LOG)) {
            statement.setTimestamp(1, Timestamp.from(action.at()));
            statement.setString(2, action.actor());
            statement.setString(3, action.description());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("permission.log", exception);
        }
    }

    @Override
    public List<PermissionAction> recentActions(int limit) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(RECENT)) {
            statement.setInt(1, limit);
            List<PermissionAction> actions = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    actions.add(new PermissionAction(rows.getTimestamp("at").toInstant(), rows.getString("actor"), rows.getString("description")));
                }
            }
            return actions;
        } catch (SQLException exception) {
            throw new PersistenceException("permission.recent_actions", exception);
        }
    }

    private static PermissionNode node(ResultSet rows) throws SQLException {
        Timestamp expires = rows.getTimestamp("expires_at");
        return new PermissionNode(
            rows.getString("permission"),
            rows.getBoolean("value"),
            Contexts.parse(rows.getString("contexts")),
            Optional.ofNullable(expires).map(Timestamp::toInstant),
            rows.getTimestamp("added_at").toInstant(),
            rows.getTimestamp("updated_at").toInstant()
        );
    }

    private static void setNode(PreparedStatement statement, int first, PermissionNode node) throws SQLException {
        statement.setString(first, node.key());
        statement.setBoolean(first + 1, node.value());
        statement.setString(first + 2, node.contexts().encode());
        if (node.expiresAt().isPresent()) {
            statement.setTimestamp(first + 3, Timestamp.from(node.expiresAt().get()));
        } else {
            statement.setNull(first + 3, Types.TIMESTAMP_WITH_TIMEZONE);
        }
        statement.setTimestamp(first + 4, Timestamp.from(node.addedAt()));
        statement.setTimestamp(first + 5, Timestamp.from(node.updatedAt()));
    }

    private static void delete(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }

    private static void run(String operation, String sql, String id) {
        try {
            delete(TransactionContext.connection(), sql, id);
        } catch (SQLException exception) {
            throw new PersistenceException(operation, exception);
        }
    }
}
