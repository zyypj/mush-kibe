package br.com.muxe.core.common.persistence.transaction;

import java.sql.Connection;

public final class TransactionContext {

    private static final ScopedValue<Connection> CONNECTION = ScopedValue.newInstance();

    private TransactionContext() {
    }

    public static Connection connection() {
        if (!CONNECTION.isBound()) {
            throw new IllegalStateException("No active transaction. Run repository calls inside Transactions.inTransaction or readOnly.");
        }
        return CONNECTION.get();
    }

    public static boolean isActive() {
        return CONNECTION.isBound();
    }

    static <T> T bind(Connection connection, TransactionalWork<T> work) throws Exception {
        return ScopedValue.where(CONNECTION, connection).call(work::execute);
    }
}
