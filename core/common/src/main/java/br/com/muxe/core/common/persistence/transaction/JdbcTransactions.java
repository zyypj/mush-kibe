package br.com.muxe.core.common.persistence.transaction;

import br.com.muxe.core.common.concurrent.MainThreadGuard;
import br.com.muxe.core.common.metrics.MetricRegistry;
import br.com.muxe.core.common.metrics.Tags;
import br.com.muxe.core.common.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class JdbcTransactions implements Transactions {

    private final DataSource dataSource;
    private final MainThreadGuard mainThreadGuard;
    private final MetricRegistry metrics;

    public JdbcTransactions(DataSource dataSource, MainThreadGuard mainThreadGuard, MetricRegistry metrics) {
        this.dataSource = dataSource;
        this.mainThreadGuard = mainThreadGuard;
        this.metrics = metrics;
    }

    @Override
    public <T> T inTransaction(String operation, TransactionalWork<T> work) {
        return execute(operation, false, work);
    }

    @Override
    public <T> T readOnly(String operation, TransactionalWork<T> work) {
        return execute(operation, true, work);
    }

    private <T> T execute(String operation, boolean readOnly, TransactionalWork<T> work) {
        if (TransactionContext.isActive()) {
            return runJoined(operation, work);
        }
        mainThreadGuard.ensureNotMainThread(operation);
        long start = System.nanoTime();
        String outcome = "error";
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setReadOnly(readOnly);
            try {
                T result = TransactionContext.bind(connection, work);
                connection.commit();
                outcome = "commit";
                return result;
            } catch (Exception exception) {
                rollbackQuietly(connection, exception);
                outcome = "rollback";
                throw propagate(operation, exception);
            }
        } catch (SQLException exception) {
            throw new PersistenceException(operation, exception);
        } finally {
            Tags tags = Tags.of("operation", operation, "outcome", outcome);
            metrics.timer("muxe_db_transaction_seconds", tags).record(System.nanoTime() - start);
        }
    }

    private static <T> T runJoined(String operation, TransactionalWork<T> work) {
        try {
            return work.execute();
        } catch (Exception exception) {
            throw propagate(operation, exception);
        }
    }

    private static void rollbackQuietly(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    private static RuntimeException propagate(String operation, Exception exception) {
        if (exception instanceof RuntimeException runtime) {
            return runtime;
        }
        return new PersistenceException(operation, exception);
    }
}
