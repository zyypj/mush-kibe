package br.com.muxe.lobby.npc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.location.StoredLocation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameNpcTest {

    private final StoredLocation location = new StoredLocation("lobby", 1, 64, 1, 0f, 0f);

    @Test
    void hologramLinesShowTheGameAndItsPlayers() {
        GameNpc npc = new GameNpc("skywars", "skywars", "SkyWars", location, "Notch", Optional.empty());

        assertThat(npc.lines(List.of("&b&l{name}", "&e{players} jogando"), 37)).containsExactly("&b&lSkyWars", "&e37 jogando");
    }

    @Test
    void idsAreLowercaseWordsSoTheyFitInCommandsAndFiles() {
        assertThat(GameNpc.isValidId("sky-wars_2")).isTrue();
        assertThat(GameNpc.isValidId("Sky Wars")).isFalse();
        assertThat(GameNpc.isValidId("")).isFalse();
        assertThatThrownBy(() -> new GameNpc("Sky Wars", "skywars", "SkyWars", location, "Notch", Optional.empty()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void movingOrRenamingKeepsTheRest() {
        GameNpc npc = new GameNpc("skywars", "skywars", "SkyWars", location, "Notch", Optional.empty());
        StoredLocation elsewhere = new StoredLocation("lobby", 5, 70, 5, 90f, 0f);

        assertThat(npc.movedTo(elsewhere).location()).isEqualTo(elsewhere);
        assertThat(npc.named("Sky Wars").name()).isEqualTo("Sky Wars");
        assertThat(npc.named("Sky Wars").group()).isEqualTo("skywars");
    }
}
