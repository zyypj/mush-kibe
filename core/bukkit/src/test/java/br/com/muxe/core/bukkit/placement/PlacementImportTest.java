package br.com.muxe.core.bukkit.placement;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PlacementImportTest {

    private static final String FILE = String.join("\n",
        "npcs:",
        "  skywars:",
        "    group: skywars",
        "    name: SkyWars",
        "    skin-name: Notch",
        "    skin:",
        "      value: abc",
        "      signature: def",
        "    location:",
        "      world: lobby",
        "      x: 1.5",
        "      y: 40.0",
        "      z: -2.5",
        "      yaw: 90.0",
        "      pitch: 0.0",
        "  broken:",
        "    name: Sem lugar",
        "registers:",
        "  caixa:",
        "    location:",
        "      world: lobby",
        "      x: 7.5",
        "      y: 33.0",
        "      z: -9.5",
        "      yaw: 180.0",
        "      pitch: 0.0",
        "    seller:",
        "      world: lobby",
        "      x: 7.5",
        "      y: 33.0",
        "      z: -8.5",
        "      yaw: 180.0",
        "      pitch: 0.0",
        "");

    @Test
    void theOldNpcFileAndTheRegisterAreReadIntoTheGroup() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(FILE);

        List<Placement> placements = PlacementImport.read(yaml, "lobby", Logger.getLogger("test"));

        assertThat(placements).containsExactly(
            new NpcPlacement("lobby", "skywars", new Position("lobby", 1.5, 40, -2.5, 90, 0), "SkyWars", NpcAction.game("skywars"), "Notch",
                Optional.of(new SkinTexture("abc", "def"))),
            new RegisterPlacement("lobby", "caixa", new Position("lobby", 7.5, 33, -9.5, 180, 0), Optional.of(new Position("lobby", 7.5, 33, -8.5, 180, 0)))
        );
    }

    @Test
    void anEmptyFileImportsNothing() {
        assertThat(PlacementImport.read(new YamlConfiguration(), "lobby", Logger.getLogger("test"))).isEmpty();
    }
}
