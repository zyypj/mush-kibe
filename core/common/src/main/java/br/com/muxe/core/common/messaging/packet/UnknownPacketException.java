package br.com.muxe.core.common.messaging.packet;

public final class UnknownPacketException extends IllegalArgumentException {

    public UnknownPacketException(String message) {
        super(message);
    }
}
