package br.com.muxe.core.common.cosmetic.infrastructure.postgres;

import br.com.muxe.core.common.cosmetic.domain.CosmeticRepository;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PostgresCosmeticRepository implements CosmeticRepository {

    private static final String OWNED = "SELECT cosmetic_id FROM core.owned_cosmetics WHERE player_id = ? AND category = ? ORDER BY acquired_at";

    private static final String OWN = """
        INSERT INTO core.owned_cosmetics (player_id, category, cosmetic_id, source, acquired_at) VALUES (?, ?, ?, ?, ?)
        ON CONFLICT DO NOTHING
        """;

    private static final String SELECTED = "SELECT cosmetic_id FROM core.selected_cosmetics WHERE player_id = ? AND category = ?";

    private static final String SELECT = """
        INSERT INTO core.selected_cosmetics (player_id, category, cosmetic_id, updated_at) VALUES (?, ?, ?, ?)
        ON CONFLICT (player_id, category) DO UPDATE SET cosmetic_id = EXCLUDED.cosmetic_id, updated_at = EXCLUDED.updated_at
        """;

    @Override
    public Set<String> owned(UUID playerId, String category) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(OWNED)) {
            statement.setObject(1, playerId);
            statement.setString(2, category);
            Set<String> owned = new LinkedHashSet<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    owned.add(rows.getString("cosmetic_id"));
                }
            }
            return owned;
        } catch (SQLException exception) {
            throw new PersistenceException("cosmetics.owned", exception);
        }
    }

    @Override
    public boolean own(UUID playerId, String category, String cosmeticId, String source, Instant at) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(OWN)) {
            statement.setObject(1, playerId);
            statement.setString(2, category);
            statement.setString(3, cosmeticId);
            statement.setString(4, source);
            statement.setTimestamp(5, Timestamp.from(at));
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new PersistenceException("cosmetics.own", exception);
        }
    }

    @Override
    public Optional<String> selected(UUID playerId, String category) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(SELECTED)) {
            statement.setObject(1, playerId);
            statement.setString(2, category);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getString("cosmetic_id")) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("cosmetics.selected", exception);
        }
    }

    @Override
    public void select(UUID playerId, String category, String cosmeticId, Instant at) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(SELECT)) {
            statement.setObject(1, playerId);
            statement.setString(2, category);
            statement.setString(3, cosmeticId);
            statement.setTimestamp(4, Timestamp.from(at));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("cosmetics.select", exception);
        }
    }
}
