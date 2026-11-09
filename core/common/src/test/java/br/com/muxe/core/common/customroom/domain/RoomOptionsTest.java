package br.com.muxe.core.common.customroom.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomOptionsTest {

    @Test
    void readsTypedValuesWithFallbacks() {
        RoomOptions options = new RoomOptions(Map.of("a", "3", "b", "0,5", "c", "true", "d", "x"));

        assertThat(options.integer("a", 1)).isEqualTo(3);
        assertThat(options.decimal("b", 1)).isEqualTo(0.5);
        assertThat(options.flag("c", false)).isTrue();
        assertThat(options.integer("d", 7)).isEqualTo(7);
        assertThat(options.integer("missing", 9)).isEqualTo(9);
    }

    @Test
    void encodingSurvivesSeparatorsAndLineBreaks() {
        RoomOptions options = new RoomOptions(Map.of("teams.1.name", "a=b\\c\nd", "shop.la.price", "4"));

        RoomOptions decoded = RoomOptions.decode(options.encode());

        assertThat(decoded).isEqualTo(options);
    }

    @Test
    void travelsInAPacket() {
        RoomOptions options = new RoomOptions(Map.of("knockback.vertical", "0.8", "events.diamond-2.enabled", "false"));
        PacketWriter out = new PacketWriter();

        RoomOptions.write(out, options);

        assertThat(RoomOptions.read(new PacketReader(out.toByteArray()))).isEqualTo(options);
    }

    @Test
    void prefixesCanBeReadAndDropped() {
        RoomOptions options = new RoomOptions(Map.of("shop.la.price", "4", "shop.la.enabled", "false", "teams.1.name", "X"));

        assertThat(options.startingWith("shop.la.")).containsOnlyKeys("price", "enabled");
        assertThat(options.withoutPrefix("shop.").values()).containsOnlyKeys("teams.1.name");
        assertThat(options.without("missing")).isSameAs(options);
    }
}
