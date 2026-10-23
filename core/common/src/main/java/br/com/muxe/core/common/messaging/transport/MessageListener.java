package br.com.muxe.core.common.messaging.transport;

@FunctionalInterface
public interface MessageListener {

    void onMessage(String channel, byte[] message);
}
