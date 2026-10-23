package br.com.muxe.core.common.messaging.bus;

import br.com.muxe.core.common.messaging.packet.Packet;
import java.util.concurrent.CompletableFuture;

public interface PacketContext<T extends Packet> {

    T packet();

    PacketMetadata metadata();

    CompletableFuture<Void> reply(Packet response);
}
