package br.com.muxe.core.common.persistence.transaction;

public interface Transactions {

    <T> T inTransaction(String operation, TransactionalWork<T> work);

    <T> T readOnly(String operation, TransactionalWork<T> work);
}
