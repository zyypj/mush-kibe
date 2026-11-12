package br.com.muxe.minigame.bedwars.arena;

import br.com.muxe.minigame.api.arena.ArenaView;
import br.com.muxe.minigame.api.match.MatchArena;
import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public record BedWarsArena(
    String name,
    String displayName,
    String mode,
    MapTemplate map,
    Point waitingSpawn,
    List<Point> waitingArea,
    Point spectator,
    int voidY,
    OptionalInt buildLimit,
    List<Point> diamonds,
    List<Point> emeralds,
    Map<TeamColor, TeamBase> teams,
    List<Point> extraShops,
    List<Point> extraUpgrades,
    List<Point> extraIron,
    List<Point> extraGold
) implements MatchArena {

    @Override
    public List<TeamColor> teamColors() {
        return List.copyOf(teams.keySet());
    }

    @Override
    public Point spawn(TeamColor team) {
        return teams.get(team).spawn();
    }

    public static BedWarsArena from(LoadedArena loaded) {
        ArenaView arena = new ArenaView(loaded.arena());
        Map<TeamColor, TeamBase> teams = arena.perTeam(color -> new TeamBase(
            color,
            arena.point(BedWarsSchema.SPAWN, color),
            arena.point(BedWarsSchema.BED, color),
            arena.points(BedWarsSchema.SHOP, color).stream().findFirst(),
            arena.points(BedWarsSchema.UPGRADES, color).stream().findFirst(),
            arena.points(BedWarsSchema.IRON, color),
            arena.points(BedWarsSchema.GOLD, color)
        ));
        return new BedWarsArena(
            arena.name(),
            arena.displayName(),
            arena.mode(),
            loaded.map(),
            arena.point(BedWarsSchema.WAITING_SPAWN),
            arena.points(BedWarsSchema.WAITING_AREA),
            arena.point(BedWarsSchema.SPECTATOR),
            arena.height(BedWarsSchema.VOID),
            arena.optionalHeight(BedWarsSchema.BUILD_LIMIT),
            arena.points(BedWarsSchema.DIAMOND),
            arena.points(BedWarsSchema.EMERALD),
            teams,
            arena.points(BedWarsSchema.EXTRA_SHOP),
            arena.points(BedWarsSchema.EXTRA_UPGRADES),
            arena.points(BedWarsSchema.EXTRA_IRON),
            arena.points(BedWarsSchema.EXTRA_GOLD)
        );
    }

    public record TeamBase(TeamColor color, Point spawn, Point bed, Optional<Point> shop, Optional<Point> upgrades, List<Point> iron, List<Point> gold) {
    }
}
