package br.com.muxe.core.bukkit.nms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PacketInterceptorTest {

    @Test
    void everyInterceptorGetsItsOwnPipelineName() {
        assertThat(PacketInterceptor.handlerName(1)).isNotEqualTo(PacketInterceptor.handlerName(2));
        assertThat(PacketInterceptor.handlerName(1)).startsWith("muxe_entity_use_");
    }
}
