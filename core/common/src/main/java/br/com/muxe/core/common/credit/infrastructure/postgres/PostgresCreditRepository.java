package br.com.muxe.core.common.credit.infrastructure.postgres;

import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

public final class PostgresCreditRepository implements CreditRepository {

    private static final String BALANCE = "SELECT balance FROM core.credits WHERE player_id = ?";

    private static final String ADD = """
        INSERT INTO core.credits (player_id, balance, updated_at) VALUES (?, ?, ?)
        ON CONFLICT (player_id) DO UPDATE SET balance = core.credits.balance + EXCLUDED.balance, updated_at = EXCLUDED.updated_at
        RETURNING balance
        """;

    private static final String SPEND = """
        UPDATE core.credits SET balance = balance - ?, updated_at = ?
        WHERE player_id = ? AND balance >= ?
        RETURNING balance
        """;

    private static final String LOG = "INSERT INTO core.credit_changes (player_id, amount, reason, changed_at) VALUES (?, ?, ?, ?)";

    @Override
    public long balance(UUID playerId) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(BALANCE)) {
            statement.setObject(1, playerId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong("balance") : 0;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("credits.balance", exception);
        }
    }

    @Override
    public long add(UUID playerId, long amount, String reason, Instant at) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(ADD)) {
            statement.setObject(1, playerId);
            statement.setLong(2, amount);
            statement.setTimestamp(3, Timestamp.from(at));
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                long balance = rows.getLong("balance");
                log(playerId, amount, reason, at);
                return balance;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("credits.add", exception);
        }
    }

    @Override
    public OptionalLong spend(UUID playerId, long amount, String reason, Instant at) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(SPEND)) {
            statement.setLong(1, amount);
            statement.setTimestamp(2, Timestamp.from(at));
            statement.setObject(3, playerId);
            statement.setLong(4, amount);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return OptionalLong.empty();
                }
                long balance = rows.getLong("balance");
                log(playerId, -amount, reason, at);
                return OptionalLong.of(balance);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("credits.spend", exception);
        }
    }

    private static void log(UUID playerId, long amount, String reason, Instant at) throws SQLException {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(LOG)) {
            statement.setObject(1, playerId);
            statement.setLong(2, amount);
            statement.setString(3, reason);
            statement.setTimestamp(4, Timestamp.from(at));
            statement.executeUpdate();
        }
    }
}
