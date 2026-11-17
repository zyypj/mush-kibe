package br.com.muxe.core.common.ultraplus.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusProfile;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgresUltraPlusRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresUltraPlusRepository repository = new PostgresUltraPlusRepository();

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "core-test", new SimpleMetricRegistry());
        new MigrationRunner(database.dataSource()).migrate("core", PostgresUltraPlusRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void savesTheMonthsTheStyleAndTheClaimedMonth() {
        UUID player = UUID.randomUUID();
        UltraPlusProfile profile = new UltraPlusProfile(player, 45, new UltraPlusStyle(Optional.of("coracao"), Optional.of("amarelo"), 2),
            Optional.of(YearMonth.of(2026, 11)));

        transactions.inTransaction("test.save", () -> {
            repository.save(UltraPlusProfile.empty(player), Instant.parse("2026-11-17T12:00:00Z"));
            repository.save(profile, Instant.parse("2026-11-17T12:05:00Z"));
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.find(player))).contains(profile);
    }

    @Test
    void aPlayerWithNothingSavedHasNoProfile() {
        assertThat(transactions.readOnly("test.find", () -> repository.find(UUID.randomUUID()))).isEmpty();
    }
}
