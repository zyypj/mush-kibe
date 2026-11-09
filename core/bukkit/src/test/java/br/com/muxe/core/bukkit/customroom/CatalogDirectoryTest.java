package br.com.muxe.core.bukkit.customroom;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.customroom.menu.MapSelector;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket.Arena;
import br.com.muxe.core.common.network.customroom.CustomRoomCatalogPacket.Mode;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CatalogDirectoryTest {

    private static final String SCHEMA = """
        root: options
        pages:
          options:
            title: "Opções"
            entries:
              size:
                type: stepper
                slot: 10
                key: team-size
        """;

    private final MutableClock clock = MutableClock.startingAt("2026-11-09T20:00:00Z");
    private final CatalogDirectory directory = new CatalogDirectory(clock, Logger.getLogger("test"));

    @Test
    void knowsTheModesAndServersOfAGameWhileTheyPublish() {
        directory.update(catalog("bedwars-1"));
        directory.update(catalog("bedwars-2"));

        assertThat(directory.has("bedwars", "solo")).isTrue();
        assertThat(directory.has("bedwars", "mega")).isFalse();
        assertThat(directory.has("skywars", "solo")).isFalse();
        assertThat(directory.servers("bedwars")).containsExactly("bedwars-1", "bedwars-2");
        assertThat(directory.schema("bedwars").orElseThrow().page("options")).isPresent();

        clock.advance(Duration.ofSeconds(31));

        assertThat(directory.servers("bedwars")).isEmpty();
        assertThat(directory.catalog("bedwars")).isEmpty();
    }

    @Test
    void theSchemaTravelsInChunks() {
        String text = "x".repeat(CustomRoomCatalogPacket.CHUNK_CHARACTERS * 2 + 5);

        List<String> chunks = CustomRoomCatalogPacket.chunks(text);

        assertThat(chunks).hasSize(3);
        assertThat(String.join("", chunks)).isEqualTo(text);
    }

    @Test
    void aClickSelectsOneMapAndShiftAddsOrRemoves() {
        assertThat(MapSelector.toggle(List.of("a", "b"), "c", false)).containsExactly("c");
        assertThat(MapSelector.toggle(List.of("c"), "c", false)).isEmpty();
        assertThat(MapSelector.toggle(List.of("a"), "b", true)).containsExactly("a", "b");
        assertThat(MapSelector.toggle(List.of("a", "b"), "a", true)).containsExactly("b");
    }

    @Test
    void theSummaryUsesTheMapNames() {
        List<Arena> arenas = List.of(new Arena("solo", "odyssea", "Odyssea"), new Arena("solo", "lago", "Lago"));

        assertThat(MapSelector.summary(List.of(), arenas, "Nenhum")).isEqualTo("Nenhum");
        assertThat(MapSelector.summary(List.of("lago", "odyssea"), arenas, "Nenhum")).isEqualTo("Lago, Odyssea");
    }

    private static CustomRoomCatalogPacket catalog(String server) {
        return new CustomRoomCatalogPacket(server, "bedwars", CustomRoomCatalogPacket.chunks(SCHEMA), List.of(new Mode("solo", "Solo", 1, 8)),
            List.of(new Arena("solo", "odyssea", "Odyssea")));
    }
}
