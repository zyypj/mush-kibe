package br.com.muxe.auth.credential.infrastructure.postgres;

import br.com.muxe.auth.credential.domain.Credential;
import br.com.muxe.auth.credential.domain.CredentialRepository;
import br.com.muxe.auth.credential.domain.PasswordHash;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.transaction.TransactionContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class PostgresCredentialRepository implements CredentialRepository {

    private static final String FIND = """
        SELECT player_id, password_hash, created_at, updated_at, last_login_at
        FROM auth.credentials
        WHERE player_id = ?
        """;

    private static final String EXISTS = "SELECT 1 FROM auth.credentials WHERE player_id = ?";

    private static final String INSERT = """
        INSERT INTO auth.credentials (player_id, password_hash, created_at, updated_at, last_login_at)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String UPDATE = """
        UPDATE auth.credentials
        SET password_hash = ?, updated_at = ?, last_login_at = ?
        WHERE player_id = ?
        """;

    @Override
    public Optional<Credential> find(UUID playerId) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(FIND)) {
            statement.setObject(1, playerId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Credential(
                    rows.getObject("player_id", UUID.class),
                    PasswordHash.parse(rows.getString("password_hash")),
                    rows.getTimestamp("created_at").toInstant(),
                    rows.getTimestamp("updated_at").toInstant(),
                    instantOrNull(rows.getTimestamp("last_login_at"))
                ));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("auth.find_credential", exception);
        }
    }

    @Override
    public boolean exists(UUID playerId) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(EXISTS)) {
            statement.setObject(1, playerId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("auth.credential_exists", exception);
        }
    }

    @Override
    public void insert(Credential credential) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(INSERT)) {
            statement.setObject(1, credential.playerId());
            statement.setString(2, credential.passwordHash().encode());
            statement.setTimestamp(3, Timestamp.from(credential.createdAt()));
            statement.setTimestamp(4, Timestamp.from(credential.updatedAt()));
            statement.setTimestamp(5, timestampOrNull(credential.lastLoginAt()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("auth.insert_credential", exception);
        }
    }

    @Override
    public void update(Credential credential) {
        try (PreparedStatement statement = TransactionContext.connection().prepareStatement(UPDATE)) {
            statement.setString(1, credential.passwordHash().encode());
            statement.setTimestamp(2, Timestamp.from(credential.updatedAt()));
            statement.setTimestamp(3, timestampOrNull(credential.lastLoginAt()));
            statement.setObject(4, credential.playerId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("auth.update_credential", exception);
        }
    }

    private static Instant instantOrNull(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Timestamp timestampOrNull(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
