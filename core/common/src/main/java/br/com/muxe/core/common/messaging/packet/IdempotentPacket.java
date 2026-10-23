package br.com.muxe.core.common.messaging.packet;

public interface IdempotentPacket extends Packet {

    String idempotencyKey();
}
