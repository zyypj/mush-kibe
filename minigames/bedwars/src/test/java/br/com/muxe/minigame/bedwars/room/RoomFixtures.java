package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.bedwars.arena.BedWarsArena;
import br.com.muxe.minigame.bedwars.arena.BedWarsSchema;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.cell.Cell;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RoomFixtures {

    public static final YamlConfiguration CONFIG = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
    public static final BedWarsSettings SETTINGS = BedWarsSettings.from(CONFIG);
    public static final GameMode SOLO = new GameMode("solo", "Solo", 6, 8, 1);
    public static final GameMode ONE_V_ONE = new GameMode("1v1", "1v1", 2, 2, 1);
    public static final GameMode DOUBLES = new GameMode("duplas", "Duplas", 2, 4, 2);

    private RoomFixtures() {
    }

    public static BedWarsArena arena(String mode, String displayName, List<TeamColor> teams) {
        ArenaDraft draft = new ArenaDraft(mode + "_test", mode, displayName);
        draft.restoreTeams(teams);
        draft.set(BedWarsSchema.WAITING_SPAWN, null, Point.block(0, 120, 0));
        draft.set(BedWarsSchema.WAITING_AREA, null, Point.block(-5, 118, -5));
        draft.set(BedWarsSchema.WAITING_AREA, null, Point.block(5, 125, 5));
        draft.set(BedWarsSchema.SPECTATOR, null, Point.block(0, 100, 0));
        draft.set(BedWarsSchema.VOID, null, Point.block(0, 40, 0));
        draft.set(BedWarsSchema.DIAMOND, null, Point.block(20, 70, 0));
        int index = 0;
        for (TeamColor team : teams) {
            int x = index++ * 40;
            draft.set(BedWarsSchema.SPAWN, team, Point.block(x, 70, 60));
            draft.set(BedWarsSchema.BED, team, Point.block(x, 70, 55));
            draft.set(BedWarsSchema.SHOP, team, Point.block(x + 3, 70, 60));
            draft.set(BedWarsSchema.UPGRADES, team, Point.block(x - 3, 70, 60));
            draft.set(BedWarsSchema.IRON, team, Point.block(x, 70, 64));
            draft.set(BedWarsSchema.GOLD, team, Point.block(x, 70, 64));
        }
        return BedWarsArena.from(new LoadedArena(draft, MapTemplate.empty()));
    }

    public static Room room(String code, GameMode mode, BedWarsArena arena) {
        return new Room(code, mode, arena, new Cell(0, 0, 0, 64));
    }
}
