package br.com.muxe.core.common.account.infrastructure.postgres;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountDetails;
import br.com.muxe.core.common.account.domain.AccountDetailsRepository;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class PostgresAccountDetailsRepository implements AccountDetailsRepository {

    private static final String SELECT = """
        SELECT a.id, a.username, a.premium, a.first_seen_at, a.last_seen_at, a.number,
               l.player_id AS located, l.country, l.region, l.city, l.asn
        FROM core.accounts a LEFT JOIN core.account_locations l ON l.player_id = a.id
        """;

    private static final String SAVE_LOCATION = """
        INSERT INTO core.account_locations (player_id, country, region, city, asn, updated_at) VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (player_id) DO UPDATE SET country = EXCLUDED.country, region = EXCLUDED.region, city = EXCLUDED.city,
            asn = EXCLUDED.asn, updated_at = EXCLUDED.updated_at
        """;

    @Override
    public Optional<AccountDetails> findById(UUID id) {
        return find(SELECT + "WHERE a.id = ?", statement -> statement.setObject(1, id));
    }

    @Override
    public Optional<AccountDetails> findByUsername(String username) {
        return find(SELECT + "WHERE LOWER(a.username) = LOWER(?)", statement -> statement.setString(1, username));
    }

    @Override
    public void saveLocation(UUID id, AccountLocation location, Instant at) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(SAVE_LOCATION)) {
            statement.setObject(1, id);
            statement.setString(2, location.country().orElse(null));
            statement.setString(3, location.region().orElse(null));
            statement.setString(4, location.city().orElse(null));
            statement.setString(5, location.asn().orElse(null));
            statement.setTimestamp(6, Timestamp.from(at));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("account.save_location", exception);
        }
    }

    private Optional<AccountDetails> find(String sql, Binder binder) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                Account account = new Account(
                    rows.getObject("id", UUID.class),
                    rows.getString("username"),
                    rows.getBoolean("premium"),
                    rows.getTimestamp("first_seen_at").toInstant(),
                    rows.getTimestamp("last_seen_at").toInstant()
                );
                Optional<AccountLocation> location = rows.getObject("located") == null ? Optional.empty() : Optional.of(new AccountLocation(
                    Optional.ofNullable(rows.getString("country")),
                    Optional.ofNullable(rows.getString("region")),
                    Optional.ofNullable(rows.getString("city")),
                    Optional.ofNullable(rows.getString("asn"))
                ));
                return Optional.of(new AccountDetails(account, rows.getLong("number"), location));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("account.find_details", exception);
        }
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
