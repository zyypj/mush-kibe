package br.com.muxe.core.common.stats.infrastructure.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.simple.SimpleMetricRegistry;
import br.com.muxe.core.common.persistence.database.Database;
import br.com.muxe.core.common.persistence.database.DatabaseConfig;
import br.com.muxe.core.common.persistence.transaction.JdbcTransactions;
import br.com.muxe.core.common.stats.domain.BedWarsTotals;
import br.com.muxe.core.common.stats.domain.ModeTotals;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class PostgresBedWarsTotalsRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static Database database;
    private static JdbcTransactions transactions;

    private final PostgresBedWarsTotalsRepository repository = new PostgresBedWarsTotalsRepository();

    @BeforeAll
    static void createTheBedWarsTable() throws SQLException {
        DatabaseConfig config = new DatabaseConfig(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(), 2, Duration.ofSeconds(5), true);
        database = Database.connect(config, "core-test", new SimpleMetricRegistry());
        transactions = new JdbcTransactions(database.dataSource(), MainThreadGuard.none(), new SimpleMetricRegistry());
        try (Connection connection = database.dataSource().getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA bedwars");
            statement.execute("""
                CREATE TABLE bedwars.player_stats (
                    player_id UUID NOT NULL, mode VARCHAR(32) NOT NULL, period VARCHAR(16) NOT NULL, period_start DATE NOT NULL,
                    games BIGINT NOT NULL DEFAULT 0, beds_broken BIGINT NOT NULL DEFAULT 0, beds_lost BIGINT NOT NULL DEFAULT 0, kills BIGINT NOT NULL DEFAULT 0,
                    deaths BIGINT NOT NULL DEFAULT 0, assists BIGINT NOT NULL DEFAULT 0, final_kills BIGINT NOT NULL DEFAULT 0,
                    final_deaths BIGINT NOT NULL DEFAULT 0, final_assists BIGINT NOT NULL DEFAULT 0, wins BIGINT NOT NULL DEFAULT 0,
                    losses BIGINT NOT NULL DEFAULT 0, winstreak BIGINT NOT NULL DEFAULT 0, best_winstreak BIGINT NOT NULL DEFAULT 0, xp BIGINT NOT NULL DEFAULT 0,
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY (player_id, mode, period, period_start))
                """);
            commit(connection);
        }
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @Test
    void readsTheLifetimeCountersOfEveryModeAndIgnoresTheShorterPeriods() throws SQLException {
        UUID player = UUID.randomUUID();
        insert(player, "geral", "total", PostgresBedWarsTotalsRepository.TOTAL_START, 8058, 5256, 4_424_587);
        insert(player, "solo", "total", PostgresBedWarsTotalsRepository.TOTAL_START, 2144, 1313, 0);
        insert(player, "solo", "diario", LocalDate.of(2026, 11, 14), 3, 2, 0);

        BedWarsTotals totals = transactions.readOnly("test.totals", () -> repository.totals(player));

        assertThat(totals.general().games()).isEqualTo(8058);
        assertThat(totals.general().wins()).isEqualTo(5256);
        assertThat(totals.general().xp()).isEqualTo(4_424_587);
        assertThat(totals.of("solo").games()).isEqualTo(2144);
        assertThat(totals.of("duplas")).isEqualTo(ModeTotals.ZERO);
        assertThat(totals.modes()).containsOnlyKeys("geral", "solo");
    }

    @Test
    void aPlayerWhoNeverPlayedHasOnlyZeros() {
        BedWarsTotals totals = transactions.readOnly("test.totals", () -> repository.totals(UUID.randomUUID()));

        assertThat(totals.general()).isEqualTo(ModeTotals.ZERO);
    }

    private static void insert(UUID player, String mode, String period, LocalDate start, long games, long wins, long xp) throws SQLException {
        try (Connection connection = database.dataSource().getConnection();
             PreparedStatement insert = connection.prepareStatement(
                 "INSERT INTO bedwars.player_stats (player_id, mode, period, period_start, games, wins, xp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            insert.setObject(1, player);
            insert.setString(2, mode);
            insert.setString(3, period);
            insert.setObject(4, start);
            insert.setLong(5, games);
            insert.setLong(6, wins);
            insert.setLong(7, xp);
            insert.executeUpdate();
            commit(connection);
        }
    }

    private static void commit(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }
}
