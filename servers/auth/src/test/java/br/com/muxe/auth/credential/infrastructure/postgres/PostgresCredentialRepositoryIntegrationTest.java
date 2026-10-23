package br.com.muxe.auth.credential.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.auth.credential.crypto.Pbkdf2PasswordHasher;
import br.com.muxe.auth.credential.domain.Credential;
import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import java.security.SecureRandom;
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
class PostgresCredentialRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresCredentialRepository repository = new PostgresCredentialRepository();
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000, new SecureRandom());

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "auth-test", new SimpleMetricRegistry());
        new MigrationRunner(database.dataSource()).migrate("auth", PostgresCredentialRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void storesAndReadsCredentials() {
        UUID player = UUID.randomUUID();
        Credential credential = Credential.register(player, hasher.hash("minhasenha"), Instant.parse("2026-01-01T00:00:00Z"));

        transactions.inTransaction("test.insert", () -> {
            repository.insert(credential);
            return null;
        });

        Credential stored = transactions.readOnly("test.find", () -> repository.find(player)).orElseThrow();
        assertThat(hasher.matches("minhasenha", stored.passwordHash())).isTrue();
        assertThat(transactions.readOnly("test.exists", () -> repository.exists(player))).isTrue();
    }

    @Test
    void updatesTheLastLogin() {
        UUID player = UUID.randomUUID();
        Credential credential = Credential.register(player, hasher.hash("minhasenha"), Instant.parse("2026-01-01T00:00:00Z"));
        Instant later = Instant.parse("2026-02-01T00:00:00Z");

        transactions.inTransaction("test.insert", () -> {
            repository.insert(credential);
            repository.update(credential.loggedIn(later));
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.find(player)))
            .map(Credential::lastLoginAt)
            .contains(later);
    }

    @Test
    void reportsUnknownPlayers() {
        assertThat(transactions.readOnly("test.exists", () -> repository.exists(UUID.randomUUID()))).isFalse();
    }
}
