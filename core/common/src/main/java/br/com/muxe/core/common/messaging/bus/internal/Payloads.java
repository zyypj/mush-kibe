package br.com.muxe.core.common.messaging.bus.internal;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.messaging.packet.Packet;
import br.com.muxe.core.common.messaging.packet.PacketType;

public final class Payloads {

    private Payloads() {
    }

    public static <T extends Packet> byte[] encode(PacketType<T> type, Packet packet) {
        PacketWriter out = new PacketWriter();
        type.codec().encode(type.javaType().cast(packet), out);
        return out.toByteArray();
    }

    public static <T extends Packet> T decode(PacketType<T> type, byte[] payload) {
        PacketReader in = new PacketReader(payload);
        T packet = type.codec().decode(in);
        in.ensureFullyRead();
        return packet;
    }
}
