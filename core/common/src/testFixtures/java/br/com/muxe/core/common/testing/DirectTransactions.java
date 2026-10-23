package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.persistence.transaction.TransactionalWork;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.util.ArrayList;
import java.util.List;

public final class DirectTransactions implements Transactions {

    private final List<String> operations = new ArrayList<>();

    @Override
    public <T> T inTransaction(String operation, TransactionalWork<T> work) {
        return run(operation, work);
    }

    @Override
    public <T> T readOnly(String operation, TransactionalWork<T> work) {
        return run(operation, work);
    }

    public List<String> operations() {
        return List.copyOf(operations);
    }

    private <T> T run(String operation, TransactionalWork<T> work) {
        operations.add(operation);
        try {
            return work.execute();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
