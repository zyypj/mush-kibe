package br.com.muxe.core.common.visibility.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgresVisibilityRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static final UUID SAVED_BY_THE_OLD_LOBBY = UUID.fromString("6b1f8f0e-4c1a-4f55-9a43-0d2f3b8f1a21");

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresVisibilityRepository repository = new PostgresVisibilityRepository();

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "core-test", new SimpleMetricRegistry());
        createTheOldLobbyTable();
        new MigrationRunner(database.dataSource()).migrate("core", PostgresVisibilityRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
    }

    private static void createTheOldLobbyTable() {
        try (Connection connection = database.dataSource().getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA lobby");
            statement.execute("CREATE TABLE lobby.player_settings (player_id UUID PRIMARY KEY, visibility VARCHAR(16) NOT NULL, updated_at TIMESTAMPTZ NOT NULL)");
            statement.execute("INSERT INTO lobby.player_settings VALUES ('" + SAVED_BY_THE_OLD_LOBBY + "', 'FRIENDS', now())");
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void savesAndUpdatesTheSettingOfAPlayer() {
        UUID player = UUID.randomUUID();

        transactions.inTransaction("test.save", () -> {
            repository.save(player, Visibility.FRIENDS, Instant.parse("2026-10-24T12:00:00Z"));
            repository.save(player, Visibility.NONE, Instant.parse("2026-10-24T12:05:00Z"));
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.find(player))).contains(Visibility.NONE);
    }

    @Test
    void settingsSavedByTheOldLobbyTableAreKept() {
        assertThat(transactions.readOnly("test.find", () -> repository.find(SAVED_BY_THE_OLD_LOBBY))).contains(Visibility.FRIENDS);
    }

    @Test
    void unknownPlayersHaveNoSetting() {
        assertThat(transactions.readOnly("test.find", () -> repository.find(UUID.randomUUID()))).isEmpty();
    }
}
