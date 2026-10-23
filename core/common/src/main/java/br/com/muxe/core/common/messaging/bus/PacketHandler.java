package br.com.muxe.core.common.messaging.bus;

import br.com.muxe.core.common.messaging.packet.Packet;

@FunctionalInterface
public interface PacketHandler<T extends Packet> {

    void handle(PacketContext<T> context) throws Exception;
}
