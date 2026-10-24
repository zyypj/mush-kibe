package br.com.muxe.lobby.visibility.infrastructure.postgres;

import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import br.com.muxe.lobby.visibility.domain.Visibility;
import br.com.muxe.lobby.visibility.domain.VisibilityRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class PostgresVisibilityRepository implements VisibilityRepository {

    private static final String FIND = "SELECT visibility FROM lobby.player_settings WHERE player_id = ?";

    private static final String SAVE = """
        INSERT INTO lobby.player_settings (player_id, visibility, updated_at)
        VALUES (?, ?, ?)
        ON CONFLICT (player_id) DO UPDATE SET visibility = EXCLUDED.visibility, updated_at = EXCLUDED.updated_at
        """;

    @Override
    public Optional<Visibility> find(UUID playerId) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(FIND)) {
            statement.setObject(1, playerId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(Visibility.parse(rows.getString("visibility"))) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("lobby.find_visibility", exception);
        }
    }

    @Override
    public void save(UUID playerId, Visibility visibility, Instant updatedAt) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(SAVE)) {
            statement.setObject(1, playerId);
            statement.setString(2, visibility.name());
            statement.setTimestamp(3, Timestamp.from(updatedAt));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("lobby.save_visibility", exception);
        }
    }
}
