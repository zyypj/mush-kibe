package br.com.muxe.lobby.visibility.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.lobby.visibility.domain.Visibility;
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

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresVisibilityRepository repository = new PostgresVisibilityRepository();

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "lobby-test", new SimpleMetricRegistry());
        new MigrationRunner(database.dataSource()).migrate("lobby", PostgresVisibilityRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
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
    void unknownPlayersHaveNoSetting() {
        assertThat(transactions.readOnly("test.find", () -> repository.find(UUID.randomUUID()))).isEmpty();
    }
}
