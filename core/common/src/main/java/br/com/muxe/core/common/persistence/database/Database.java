package br.com.muxe.core.common.persistence.database;

import br.com.muxe.core.common.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

public final class Database implements AutoCloseable {

    private final HikariDataSource dataSource;

    private Database(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static Database connect(DatabaseConfig config, String applicationName, MetricRegistry metrics) {
        PGSimpleDataSource postgres = new PGSimpleDataSource();
        postgres.setUrl(config.url());
        postgres.setUser(config.username());
        postgres.setPassword(config.password());
        postgres.setApplicationName(applicationName);

        HikariConfig hikari = new HikariConfig();
        hikari.setDataSource(postgres);
        hikari.setPoolName("muxe-" + applicationName);
        hikari.setMaximumPoolSize(config.poolSize());
        hikari.setMinimumIdle(Math.min(2, config.poolSize()));
        hikari.setConnectionTimeout(config.connectionTimeout().toMillis());
        hikari.setAutoCommit(true);

        Database database = new Database(new HikariDataSource(hikari));
        database.registerMetrics(metrics);
        return database;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    private void registerMetrics(MetricRegistry metrics) {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        metrics.gauge("muxe_db_connections_active", pool::getActiveConnections);
        metrics.gauge("muxe_db_connections_idle", pool::getIdleConnections);
        metrics.gauge("muxe_db_connections_pending", pool::getThreadsAwaitingConnection);
        metrics.gauge("muxe_db_connections_total", pool::getTotalConnections);
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
