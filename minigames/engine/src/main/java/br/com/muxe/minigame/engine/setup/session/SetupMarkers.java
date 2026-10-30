package br.com.muxe.minigame.engine.setup.session;

import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.setup.Point;
import br.com.muxe.minigame.engine.setup.SetupSchema;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.setup.StepKind;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class SetupMarkers {

    private final TextHolograms holograms;
    private final Map<UUID, List<SpawnedHologram>> shown = new HashMap<>();

    SetupMarkers(TextHolograms holograms) {
        this.holograms = holograms;
    }

    static List<Marker> markers(SetupSchema schema, ArenaDraft draft, SetupHelp help) {
        List<Marker> markers = new ArrayList<>();
        for (SetupStep step : schema.steps(draft.mode())) {
            if (step.kind() == StepKind.HEIGHT) {
                continue;
            }
            List<TeamColor> teams = step.perTeam() ? draft.teams() : Collections.singletonList(null);
            for (TeamColor team : teams) {
                List<Point> points = draft.points(step, team);
                for (int index = 0; index < points.size(); index++) {
                    markers.add(new Marker(points.get(index), lines(step, team, index + 1, help), height(step.kind())));
                }
            }
        }
        return markers;
    }

    void show(Player viewer, World world, List<Marker> markers) {
        hide(viewer);
        List<SpawnedHologram> spawned = new ArrayList<>(markers.size());
        for (Marker marker : markers) {
            spawned.add(holograms.show(viewer, Points.toLocation(marker.point(), world).add(0, marker.height(), 0), marker.lines()));
        }
        shown.put(viewer.getUniqueId(), spawned);
    }

    void resend(Player viewer) {
        shown.getOrDefault(viewer.getUniqueId(), List.of()).forEach(SpawnedHologram::resend);
    }

    void hide(Player viewer) {
        List<SpawnedHologram> previous = shown.remove(viewer.getUniqueId());
        if (previous != null) {
            previous.forEach(SpawnedHologram::remove);
        }
    }

    void forget(UUID viewer) {
        shown.remove(viewer);
    }

    private static List<String> lines(SetupStep step, TeamColor team, int index, SetupHelp help) {
        if (step.hologram().isEmpty()) {
            return List.of("&e" + help.label(step, team));
        }
        String teamName = team == null ? "" : team.colored();
        return step.hologram().stream().map(line -> line.replace("{team}", teamName).replace("{index}", String.valueOf(index))).toList();
    }

    static double height(StepKind kind) {
        return switch (kind) {
            case BLOCK -> 1.9;
            case REGION -> 1.4;
            default -> 2.6;
        };
    }

    record Marker(Point point, List<String> lines, double height) {
    }
}
