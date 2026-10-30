package br.com.muxe.minigame.bedwars.arena;

import br.com.muxe.minigame.engine.arena.LoadedArena;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.team.TeamColor;
import br.com.muxe.minigame.engine.world.map.MapTemplate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
) {

    public static BedWarsArena from(LoadedArena loaded) {
        ArenaDraft arena = loaded.arena();
        Map<TeamColor, TeamBase> teams = new LinkedHashMap<>();
        for (TeamColor color : arena.teams()) {
            teams.put(color, new TeamBase(
                color,
                single(arena, BedWarsSchema.SPAWN, color),
                single(arena, BedWarsSchema.BED, color),
                single(arena, BedWarsSchema.SHOP, color),
                single(arena, BedWarsSchema.UPGRADES, color),
                arena.points(BedWarsSchema.IRON, color),
                arena.points(BedWarsSchema.GOLD, color)
            ));
        }
        double limit = arena.number(BedWarsSchema.BUILD_LIMIT.id()).orElse(Double.NaN);
        return new BedWarsArena(
            arena.name(),
            arena.displayName(),
            arena.mode(),
            loaded.map(),
            single(arena, BedWarsSchema.WAITING_SPAWN, null),
            arena.points(BedWarsSchema.WAITING_AREA, null),
            single(arena, BedWarsSchema.SPECTATOR, null),
            (int) arena.number(BedWarsSchema.VOID.id()).orElse(0),
            Double.isNaN(limit) ? OptionalInt.empty() : OptionalInt.of((int) limit),
            arena.points(BedWarsSchema.DIAMOND, null),
            arena.points(BedWarsSchema.EMERALD, null),
            teams,
            arena.points(BedWarsSchema.EXTRA_SHOP.id()),
            arena.points(BedWarsSchema.EXTRA_UPGRADES.id()),
            arena.points(BedWarsSchema.EXTRA_IRON.id()),
            arena.points(BedWarsSchema.EXTRA_GOLD.id())
        );
    }

    private static Point single(ArenaDraft arena, SetupStep step, TeamColor team) {
        List<Point> points = arena.points(step, team);
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Arena " + arena.name() + " has no " + ArenaDraft.key(step, team));
        }
        return points.getFirst();
    }

    public record TeamBase(TeamColor color, Point spawn, Point bed, Point shop, Point upgrades, List<Point> iron, List<Point> gold) {
    }
}
