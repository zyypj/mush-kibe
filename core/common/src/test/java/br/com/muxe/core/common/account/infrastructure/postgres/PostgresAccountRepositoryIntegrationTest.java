package br.com.muxe.core.common.account.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.PersistenceException;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.migration.MigrationReport;
import br.com.muxe.core.common.persistence.migration.MigrationRunner;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgresAccountRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static Database database;
    private static JdbcTransactions transactions;
    private static MigrationReport migration;

    private final PostgresAccountRepository repository = new PostgresAccountRepository();

    @BeforeAll
    static void migrate() {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "test", new SimpleMetricRegistry());
        migration = new MigrationRunner(database.dataSource()).migrate("core", PostgresAccountRepositoryIntegrationTest.class.getClassLoader());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void migrationsCreateTheCoreSchema() {
        assertThat(migration.schemaVersion()).isEqualTo("10");
    }

    @Test
    void aNameCanBeReleasedAndTakenByAnotherAccountInTheSameTransaction() {
        String name = "Grumm" + suffix();
        Account previous = Account.firstLogin(UUID.randomUUID(), name, true, now());
        Account next = Account.firstLogin(UUID.randomUUID(), name, true, now());
        transactions.inTransaction("test.save", () -> {
            repository.save(previous);
            return null;
        });

        transactions.inTransaction("test.reclaim", () -> {
            repository.save(previous.withoutUsername());
            repository.save(next);
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.findByUsername(name))).map(Account::id).contains(next.id());
        assertThat(transactions.readOnly("test.find", () -> repository.findById(previous.id())))
            .hasValueSatisfying(account -> assertThat(account.currentUsername()).isEmpty());
    }

    @Test
    void savesAndFindsAccountsIgnoringCase() {
        Account account = Account.firstLogin(UUID.randomUUID(), "Notch" + suffix(), false, now());

        transactions.inTransaction("test.save", () -> {
            repository.save(account);
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.findById(account.id()))).contains(account);
        assertThat(transactions.readOnly("test.find", () -> repository.findByUsername(account.username().toUpperCase()))).contains(account);
    }

    @Test
    void storesThePremiumFlagAndNeverChangesIt() {
        Account account = Account.firstLogin(UUID.randomUUID(), "Dinnerbone" + suffix(), true, now());

        transactions.inTransaction("test.save", () -> {
            repository.save(account);
            repository.save(new Account(account.id(), account.username(), false, account.firstSeenAt(), now()));
            return null;
        });

        assertThat(transactions.readOnly("test.find", () -> repository.findById(account.id())))
            .map(Account::premium)
            .contains(true);
    }

    @Test
    void upsertKeepsTheFirstSeenDate() {
        Account account = Account.firstLogin(UUID.randomUUID(), "Jeb" + suffix(), false, now().minus(1, ChronoUnit.DAYS));
        transactions.inTransaction("test.save", () -> {
            repository.save(account);
            repository.save(account.withLogin(account.username(), now()));
            return null;
        });

        Account stored = transactions.readOnly("test.find", () -> repository.findById(account.id())).orElseThrow();

        assertThat(stored.firstSeenAt()).isEqualTo(account.firstSeenAt());
        assertThat(stored.lastSeenAt()).isEqualTo(now());
    }

    @Test
    void theDatabaseRefusesTwoAccountsWithTheSameNameInAnotherCase() {
        String name = "Alex" + suffix();
        transactions.inTransaction("test.save", () -> {
            repository.save(Account.firstLogin(UUID.randomUUID(), name, false, now()));
            return null;
        });

        assertThatThrownBy(() -> transactions.inTransaction("test.save", () -> {
            repository.save(Account.firstLogin(UUID.randomUUID(), name.toLowerCase(), false, now()));
            return null;
        })).isInstanceOf(PersistenceException.class);
    }

    @Test
    void rollsBackWhenTheWorkFails() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> transactions.inTransaction("test.rollback", () -> {
            repository.save(Account.firstLogin(id, "Herobrine" + suffix(), false, now()));
            throw new IllegalStateException("abort");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(transactions.readOnly("test.find", () -> repository.findById(id))).isEmpty();
    }

    private static Instant now() {
        return Instant.parse("2026-01-02T03:04:05Z");
    }

    private static String suffix() {
        return Integer.toString(Math.abs(UUID.randomUUID().hashCode() % 10_000));
    }
}
