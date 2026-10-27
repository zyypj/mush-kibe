package br.com.muxe.core.bukkit.placement;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.ParkourPointPlacement;
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
    void theParkourPointsAreReadIntoTheGroup() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
            "parkour:",
            "  start:",
            "    world: lobby",
            "    x: 28.5",
            "    y: 34.0",
            "    z: -8.5",
            "    yaw: 0.0",
            "    pitch: 0.0",
            "  checkpoints:",
            "    '2':",
            "      world: lobby",
            "      x: -56.5",
            "      y: 87.0",
            "      z: -58.5",
            "      yaw: 180.0",
            "      pitch: 0.0",
            "    zero:",
            "      world: lobby",
            "  end:",
            "    world: lobby",
            "    x: 48.5",
            "    y: 118.0",
            "    z: -73.5",
            "    yaw: 180.0",
            "    pitch: 0.0",
            ""));

        List<Placement> placements = PlacementImport.read(yaml, "lobby", Logger.getLogger("test"));

        assertThat(placements).containsExactly(
            new ParkourPointPlacement("lobby", ParkourPoint.start(), new Position("lobby", 28.5, 34, -8.5, 0, 0)),
            new ParkourPointPlacement("lobby", ParkourPoint.checkpoint(2), new Position("lobby", -56.5, 87, -58.5, 180, 0)),
            new ParkourPointPlacement("lobby", ParkourPoint.end(), new Position("lobby", 48.5, 118, -73.5, 180, 0))
        );
    }

    @Test
    void anEmptyFileImportsNothing() {
        assertThat(PlacementImport.read(new YamlConfiguration(), "lobby", Logger.getLogger("test"))).isEmpty();
    }
}
