package br.com.muxe.lobby.npc;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.location.StoredLocation;
import br.com.muxe.core.bukkit.npc.NpcSkin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NpcStoreTest {

    @TempDir
    private Path folder;

    @Test
    void npcsSurviveBeingSavedAndLoaded() {
        NpcStore store = new NpcStore(folder.resolve("npcs.yml").toFile(), Logger.getAnonymousLogger());
        GameNpc skywars = new GameNpc("skywars", "skywars", "SkyWars", new StoredLocation("lobby", 1.5, 64, -2.5, 90f, 0f), "Notch", Optional.of(new NpcSkin("dmFsdWU=", "c2ln")));
        GameNpc bedwars = new GameNpc("bedwars", "bedwars", "BedWars", new StoredLocation("lobby", 4.5, 64, -2.5, 270f, 0f), "", Optional.empty());

        store.save(List.of(skywars, bedwars));
        Map<String, GameNpc> loaded = store.load();

        assertThat(loaded).containsOnlyKeys("skywars", "bedwars");
        assertThat(loaded.get("skywars")).isEqualTo(skywars);
        assertThat(loaded.get("bedwars").skin()).isEmpty();
    }

    @Test
    void aMissingFileMeansNoNpcs() {
        assertThat(new NpcStore(folder.resolve("missing.yml").toFile(), Logger.getAnonymousLogger()).load()).isEmpty();
    }

    @Test
    void brokenEntriesAreSkippedInsteadOfBreakingTheLobby() throws IOException {
        File file = folder.resolve("npcs.yml").toFile();
        Files.writeString(file.toPath(), String.join("\n",
            "broken:",
            "  group: skywars",
            "Wrong Id:",
            "  group: skywars",
            "  location:",
            "    world: lobby",
            "    x: 0",
            "    y: 0",
            "    z: 0",
            "    yaw: 0",
            "    pitch: 0"
        ));

        assertThat(new NpcStore(file, Logger.getAnonymousLogger()).load()).isEmpty();
    }
}
