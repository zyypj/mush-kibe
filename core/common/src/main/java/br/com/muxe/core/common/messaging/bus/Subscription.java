package br.com.muxe.core.common.messaging.bus;

@FunctionalInterface
public interface Subscription {

    void cancel();
}
