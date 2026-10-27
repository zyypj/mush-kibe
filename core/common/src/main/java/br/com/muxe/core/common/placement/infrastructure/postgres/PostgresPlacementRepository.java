package br.com.muxe.core.common.placement.infrastructure.postgres;

import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.PlacementRepository;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import br.com.muxe.core.common.placement.domain.SocialHeadPlacement;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class PostgresPlacementRepository implements PlacementRepository {

    private static final String POSITION = "world, x, y, z, yaw, pitch";

    private static final String FIND_NPCS = "SELECT id, " + POSITION
        + ", name, action, action_target, skin_name, skin_value, skin_signature FROM core.npc_placements WHERE server_group = ? ORDER BY id";

    private static final String FIND_HOLOGRAMS = "SELECT id, " + POSITION + ", lines FROM core.hologram_placements WHERE server_group = ? ORDER BY id";

    private static final String FIND_REGISTERS = "SELECT id, " + POSITION
        + ", seller_world, seller_x, seller_y, seller_z, seller_yaw, seller_pitch FROM core.register_placements WHERE server_group = ? ORDER BY id";

    private static final String FIND_SOCIAL_HEADS = "SELECT id, " + POSITION + " FROM core.social_head_placements WHERE server_group = ? ORDER BY id";

    private static final String SAVE_NPC = """
        INSERT INTO core.npc_placements (server_group, id, world, x, y, z, yaw, pitch, name, action, action_target, skin_name, skin_value, skin_signature)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (server_group, id) DO UPDATE
        SET world = EXCLUDED.world, x = EXCLUDED.x, y = EXCLUDED.y, z = EXCLUDED.z, yaw = EXCLUDED.yaw, pitch = EXCLUDED.pitch,
            name = EXCLUDED.name, action = EXCLUDED.action, action_target = EXCLUDED.action_target,
            skin_name = EXCLUDED.skin_name, skin_value = EXCLUDED.skin_value, skin_signature = EXCLUDED.skin_signature
        """;

    private static final String SAVE_HOLOGRAM = """
        INSERT INTO core.hologram_placements (server_group, id, world, x, y, z, yaw, pitch, lines)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (server_group, id) DO UPDATE
        SET world = EXCLUDED.world, x = EXCLUDED.x, y = EXCLUDED.y, z = EXCLUDED.z, yaw = EXCLUDED.yaw, pitch = EXCLUDED.pitch,
            lines = EXCLUDED.lines
        """;

    private static final String SAVE_REGISTER = """
        INSERT INTO core.register_placements (server_group, id, world, x, y, z, yaw, pitch, seller_world, seller_x, seller_y, seller_z, seller_yaw, seller_pitch)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (server_group, id) DO UPDATE
        SET world = EXCLUDED.world, x = EXCLUDED.x, y = EXCLUDED.y, z = EXCLUDED.z, yaw = EXCLUDED.yaw, pitch = EXCLUDED.pitch,
            seller_world = EXCLUDED.seller_world, seller_x = EXCLUDED.seller_x, seller_y = EXCLUDED.seller_y,
            seller_z = EXCLUDED.seller_z, seller_yaw = EXCLUDED.seller_yaw, seller_pitch = EXCLUDED.seller_pitch
        """;

    private static final String SAVE_SOCIAL_HEAD = """
        INSERT INTO core.social_head_placements (server_group, id, world, x, y, z, yaw, pitch)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (server_group, id) DO UPDATE
        SET world = EXCLUDED.world, x = EXCLUDED.x, y = EXCLUDED.y, z = EXCLUDED.z, yaw = EXCLUDED.yaw, pitch = EXCLUDED.pitch
        """;

    @Override
    public Placements findByGroup(String group) {
        try {
            Connection connection = TransactionContext.connection();
            return new Placements(npcs(connection, group), holograms(connection, group), registers(connection, group), socialHeads(connection, group));
        } catch (SQLException exception) {
            throw new PersistenceException("placement.find_by_group", exception);
        }
    }

    @Override
    public void save(Placement placement) {
        try {
            Connection connection = TransactionContext.connection();
            switch (placement) {
                case NpcPlacement npc -> saveNpc(connection, npc);
                case HologramPlacement hologram -> saveHologram(connection, hologram);
                case RegisterPlacement register -> saveRegister(connection, register);
                case SocialHeadPlacement head -> saveSocialHead(connection, head);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("placement.save", exception);
        }
    }

    @Override
    public boolean remove(PlacementKind kind, String group, String id) {
        String table = switch (kind) {
            case NPC -> "core.npc_placements";
            case HOLOGRAM -> "core.hologram_placements";
            case REGISTER -> "core.register_placements";
            case SOCIAL_HEAD -> "core.social_head_placements";
        };
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement("DELETE FROM " + table + " WHERE server_group = ? AND id = ?")) {
            statement.setString(1, group);
            statement.setString(2, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new PersistenceException("placement.remove", exception);
        }
    }

    private static List<NpcPlacement> npcs(Connection connection, String group) throws SQLException {
        List<NpcPlacement> npcs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_NPCS)) {
            statement.setString(1, group);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String value = rows.getString("skin_value");
                    String signature = rows.getString("skin_signature");
                    Optional<SkinTexture> skin = value == null || signature == null ? Optional.empty() : Optional.of(new SkinTexture(value, signature));
                    npcs.add(new NpcPlacement(
                        group,
                        rows.getString("id"),
                        position(rows, ""),
                        rows.getString("name"),
                        new NpcAction(NpcAction.Type.valueOf(rows.getString("action")), rows.getString("action_target")),
                        rows.getString("skin_name"),
                        skin
                    ));
                }
            }
        }
        return npcs;
    }

    private static List<HologramPlacement> holograms(Connection connection, String group) throws SQLException {
        List<HologramPlacement> holograms = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_HOLOGRAMS)) {
            statement.setString(1, group);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Array lines = rows.getArray("lines");
                    holograms.add(new HologramPlacement(group, rows.getString("id"), position(rows, ""), Arrays.asList((String[]) lines.getArray())));
                }
            }
        }
        return holograms;
    }

    private static List<RegisterPlacement> registers(Connection connection, String group) throws SQLException {
        List<RegisterPlacement> registers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_REGISTERS)) {
            statement.setString(1, group);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Optional<Position> seller = rows.getString("seller_world") == null ? Optional.empty() : Optional.of(position(rows, "seller_"));
                    registers.add(new RegisterPlacement(group, rows.getString("id"), position(rows, ""), seller));
                }
            }
        }
        return registers;
    }

    private static List<SocialHeadPlacement> socialHeads(Connection connection, String group) throws SQLException {
        List<SocialHeadPlacement> heads = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FIND_SOCIAL_HEADS)) {
            statement.setString(1, group);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    heads.add(new SocialHeadPlacement(group, rows.getString("id"), position(rows, "")));
                }
            }
        }
        return heads;
    }

    private static void saveNpc(Connection connection, NpcPlacement npc) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SAVE_NPC)) {
            statement.setString(1, npc.group());
            statement.setString(2, npc.id());
            setPosition(statement, 3, npc.position());
            statement.setString(9, npc.name());
            statement.setString(10, npc.action().type().name());
            statement.setString(11, npc.action().target());
            statement.setString(12, npc.skinName());
            statement.setString(13, npc.skin().map(SkinTexture::value).orElse(null));
            statement.setString(14, npc.skin().map(SkinTexture::signature).orElse(null));
            statement.executeUpdate();
        }
    }

    private static void saveHologram(Connection connection, HologramPlacement hologram) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SAVE_HOLOGRAM)) {
            statement.setString(1, hologram.group());
            statement.setString(2, hologram.id());
            setPosition(statement, 3, hologram.position());
            statement.setArray(9, connection.createArrayOf("text", hologram.lines().toArray()));
            statement.executeUpdate();
        }
    }

    private static void saveRegister(Connection connection, RegisterPlacement register) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SAVE_REGISTER)) {
            statement.setString(1, register.group());
            statement.setString(2, register.id());
            setPosition(statement, 3, register.position());
            if (register.seller().isPresent()) {
                setPosition(statement, 9, register.seller().get());
            } else {
                statement.setNull(9, Types.VARCHAR);
                statement.setNull(10, Types.DOUBLE);
                statement.setNull(11, Types.DOUBLE);
                statement.setNull(12, Types.DOUBLE);
                statement.setNull(13, Types.REAL);
                statement.setNull(14, Types.REAL);
            }
            statement.executeUpdate();
        }
    }

    private static void saveSocialHead(Connection connection, SocialHeadPlacement head) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SAVE_SOCIAL_HEAD)) {
            statement.setString(1, head.group());
            statement.setString(2, head.id());
            setPosition(statement, 3, head.position());
            statement.executeUpdate();
        }
    }

    private static Position position(ResultSet rows, String prefix) throws SQLException {
        return new Position(
            rows.getString(prefix + "world"),
            rows.getDouble(prefix + "x"),
            rows.getDouble(prefix + "y"),
            rows.getDouble(prefix + "z"),
            rows.getFloat(prefix + "yaw"),
            rows.getFloat(prefix + "pitch")
        );
    }

    private static void setPosition(PreparedStatement statement, int first, Position position) throws SQLException {
        statement.setString(first, position.world());
        statement.setDouble(first + 1, position.x());
        statement.setDouble(first + 2, position.y());
        statement.setDouble(first + 3, position.z());
        statement.setFloat(first + 4, position.yaw());
        statement.setFloat(first + 5, position.pitch());
    }
}
