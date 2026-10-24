package br.com.muxe.core.common.account.infrastructure.postgres;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountRepository;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

public final class PostgresAccountRepository implements AccountRepository {

    private static final String SELECT = "SELECT id, username, premium, first_seen_at, last_seen_at FROM core.accounts ";

    private static final String FIND_BY_ID = SELECT + "WHERE id = ?";

    private static final String FIND_BY_USERNAME = SELECT + "WHERE LOWER(username) = LOWER(?)";

    private static final String UPSERT = """
        INSERT INTO core.accounts (id, username, premium, first_seen_at, last_seen_at)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE
        SET username = EXCLUDED.username, last_seen_at = EXCLUDED.last_seen_at
        """;

    @Override
    public Optional<Account> findById(UUID id) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(FIND_BY_ID)) {
            statement.setObject(1, id);
            return single(statement);
        } catch (SQLException exception) {
            throw new PersistenceException("account.find_by_id", exception);
        }
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(FIND_BY_USERNAME)) {
            statement.setString(1, username);
            return single(statement);
        } catch (SQLException exception) {
            throw new PersistenceException("account.find_by_username", exception);
        }
    }

    @Override
    public void save(Account account) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(UPSERT)) {
            statement.setObject(1, account.id());
            statement.setString(2, account.username());
            statement.setBoolean(3, account.premium());
            statement.setTimestamp(4, Timestamp.from(account.firstSeenAt()));
            statement.setTimestamp(5, Timestamp.from(account.lastSeenAt()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("account.save", exception);
        }
    }

    private static Optional<Account> single(PreparedStatement statement) throws SQLException {
        try (ResultSet rows = statement.executeQuery()) {
            if (!rows.next()) {
                return Optional.empty();
            }
            return Optional.of(new Account(
                rows.getObject("id", UUID.class),
                rows.getString("username"),
                rows.getBoolean("premium"),
                rows.getTimestamp("first_seen_at").toInstant(),
                rows.getTimestamp("last_seen_at").toInstant()
            ));
        }
    }
}
