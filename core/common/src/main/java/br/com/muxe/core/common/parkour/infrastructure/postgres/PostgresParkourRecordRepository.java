package br.com.muxe.core.common.parkour.infrastructure.postgres;

import br.com.muxe.core.common.parkour.domain.ParkourRecordRepository;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class PostgresParkourRecordRepository implements ParkourRecordRepository {

    private static final String FIND = "SELECT best_millis FROM core.parkour_records WHERE server_group = ? AND player_id = ?";

    private static final String SAVE = """
        INSERT INTO core.parkour_records (server_group, player_id, best_millis, achieved_at)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (server_group, player_id) DO UPDATE
        SET best_millis = EXCLUDED.best_millis, achieved_at = EXCLUDED.achieved_at
        WHERE core.parkour_records.best_millis > EXCLUDED.best_millis
        """;

    @Override
    public Optional<Duration> best(String group, UUID playerId) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(FIND)) {
            statement.setString(1, group);
            statement.setObject(2, playerId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(Duration.ofMillis(rows.getLong("best_millis"))) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("parkour.find_record", exception);
        }
    }

    @Override
    public void save(String group, UUID playerId, Duration best, Instant achievedAt) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(SAVE)) {
            statement.setString(1, group);
            statement.setObject(2, playerId);
            statement.setLong(3, best.toMillis());
            statement.setTimestamp(4, Timestamp.from(achievedAt));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("parkour.save_record", exception);
        }
    }
}
