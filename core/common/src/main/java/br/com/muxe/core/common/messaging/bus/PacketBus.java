package br.com.muxe.core.common.messaging.bus;

import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface PacketBus extends AutoCloseable {

    CompletableFuture<Void> publish(PacketTarget target, Packet packet);

    <R extends Packet> CompletableFuture<R> request(PacketTarget target, Packet request, Class<R> responseType, Duration timeout);

    <T extends Packet> Subscription subscribe(PacketType<T> type, PacketHandler<T> handler);

    void start();

    @Override
    void close();
}
