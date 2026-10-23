package br.com.muxe.core.common.messaging.codec;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface PacketCodec<T> {

    void encode(T value, PacketWriter out);

    T decode(PacketReader in);

    static <T> PacketCodec<T> of(BiConsumer<T, PacketWriter> encoder, Function<PacketReader, T> decoder) {
        return new PacketCodec<>() {
            @Override
            public void encode(T value, PacketWriter out) {
                encoder.accept(value, out);
            }

            @Override
            public T decode(PacketReader in) {
                return decoder.apply(in);
            }
        };
    }
}
