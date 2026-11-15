package br.com.muxe.core.common.preference.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.core.common.preference.domain.Preference;
import br.com.muxe.core.common.preference.domain.Preferences;
import br.com.muxe.core.common.preference.domain.PrivateMessages;
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
class PostgresPreferenceRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresPreferenceRepository repository = new PostgresPreferenceRepository();

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "core-test", new SimpleMetricRegistry());
        new MigrationRunner(database.dataSource()).migrate("core", PostgresPreferenceRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void savesEachPreferenceAndTheLastValueWins() {
        UUID player = UUID.randomUUID();

        transactions.inTransaction("test.save", () -> {
            repository.save(player, Preference.CHAT, "false", Instant.parse("2026-11-15T12:00:00Z"));
            repository.save(player, Preference.PRIVATE_MESSAGES, "FRIENDS", Instant.parse("2026-11-15T12:00:00Z"));
            repository.save(player, Preference.CHAT, "true", Instant.parse("2026-11-15T12:05:00Z"));
            return null;
        });

        Preferences preferences = transactions.readOnly("test.find", () -> repository.find(player));
        assertThat(preferences.on(Preference.CHAT)).isTrue();
        assertThat(preferences.privateMessages()).isEqualTo(PrivateMessages.FRIENDS);
        assertThat(preferences.values()).hasSize(2);
    }

    @Test
    void aPlayerWithNothingSavedHasTheDefaults() {
        assertThat(transactions.readOnly("test.find", () -> repository.find(UUID.randomUUID()))).isEqualTo(Preferences.DEFAULTS);
    }
}
