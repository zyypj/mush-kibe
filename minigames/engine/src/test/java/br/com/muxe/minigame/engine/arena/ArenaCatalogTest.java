package br.com.muxe.minigame.engine.arena;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.ArenaFiles;
import br.com.muxe.minigame.engine.world.cell.MatchCells;
import br.com.muxe.minigame.engine.world.map.MapFiles;
import br.com.muxe.minigame.engine.world.map.MapFixtures;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArenaCatalogTest {

    @TempDir
    Path folder;

    @Test
    void onlyReadyArenasWithAMapThatFitsACellArePlayable() throws IOException {
        ArenaFiles files = new ArenaFiles(folder.resolve("arenas"));
        Path maps = folder.resolve("maps");
        MapTemplate small = MapTemplate.builder().put(MapFixtures.chunk(0, 0, MapFixtures.section(4, 1, 0))).build();
        MapTemplate wide = MapTemplate.builder().put(MapFixtures.chunk(40, 0, MapFixtures.section(4, 1, 0))).build();
        arena(files, maps, "1v1_slip", "1v1", "Slip", true, small);
        arena(files, maps, "1v1_alaska", "1v1", "Alaska", true, small);
        arena(files, maps, "1v1_draft", "1v1", "Rascunho", false, small);
        arena(files, maps, "1v1_huge", "1v1", "Enorme", true, wide);
        arena(files, maps, "solo_tulum", "solo", "Tulum", true, small);
        files.write(new ArenaDraft("1v1_nomap", "1v1", "Sem mapa"), true);
        MatchCells cells = new MatchCells(64, 4, 12);
        ArenaCatalog catalog = new ArenaCatalog(files, maps, cells::fits, Logger.getLogger("test"));

        assertThat(catalog.loadAll()).isEqualTo(3);
        assertThat(catalog.ofMode("1v1")).extracting(LoadedArena::displayName).containsExactly("Alaska", "Slip");
        assertThat(catalog.find("solo_tulum")).isPresent();
        assertThat(catalog.find("1v1_draft")).isEmpty();
    }

    @Test
    void reloadingPicksUpAnArenaThatBecameReady() throws IOException {
        ArenaFiles files = new ArenaFiles(folder.resolve("arenas"));
        Path maps = folder.resolve("maps");
        MapTemplate map = MapTemplate.builder().put(MapFixtures.chunk(0, 0, MapFixtures.section(4, 1, 0))).build();
        arena(files, maps, "1v1_slip", "1v1", "Slip", false, map);
        ArenaCatalog catalog = new ArenaCatalog(files, maps, ignored -> true, Logger.getLogger("test"));
        catalog.loadAll();

        files.write(new ArenaDraft("1v1_slip", "1v1", "Slip"), true);

        assertThat(catalog.reload("1v1_slip")).isPresent();
        assertThat(catalog.ofMode("1v1")).hasSize(1);
    }

    private static void arena(ArenaFiles files, Path maps, String name, String mode, String display, boolean ready, MapTemplate map) throws IOException {
        files.write(new ArenaDraft(name, mode, display), ready);
        MapFiles.write(maps.resolve(name + MapFiles.EXTENSION), map);
    }
}
