package br.com.muxe.core.common.persistence.transaction;

@FunctionalInterface
public interface TransactionalWork<T> {

    T execute() throws Exception;
}
