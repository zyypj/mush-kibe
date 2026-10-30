package br.com.muxe.minigame.engine.team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeamAssigner {

    private TeamAssigner() {
    }

    public static Map<TeamColor, List<UUID>> assign(List<TeamColor> teams, int teamSize, List<UUID> players, List<List<UUID>> parties) {
        Map<TeamColor, List<UUID>> result = new LinkedHashMap<>();
        teams.forEach(team -> result.put(team, new ArrayList<>()));
        Set<UUID> placed = new HashSet<>();
        List<List<UUID>> groups = new ArrayList<>();
        for (List<UUID> party : parties) {
            List<UUID> present = party.stream().filter(players::contains).filter(placed::add).toList();
            if (present.size() > 1) {
                groups.add(present);
            } else {
                present.forEach(placed::remove);
            }
        }
        groups.sort(Comparator.comparingInt((List<UUID> group) -> group.size()).reversed());
        for (List<UUID> group : groups) {
            List<UUID> remaining = new ArrayList<>(group);
            while (!remaining.isEmpty()) {
                TeamColor target = emptiest(result, teamSize);
                if (target == null) {
                    break;
                }
                List<UUID> members = result.get(target);
                while (!remaining.isEmpty() && members.size() < teamSize) {
                    members.add(remaining.removeFirst());
                }
            }
        }
        for (UUID player : players) {
            if (placed.contains(player)) {
                continue;
            }
            TeamColor target = emptiest(result, teamSize);
            if (target == null) {
                break;
            }
            result.get(target).add(player);
            placed.add(player);
        }
        return result;
    }

    private static TeamColor emptiest(Map<TeamColor, List<UUID>> teams, int teamSize) {
        TeamColor best = null;
        int fewest = Integer.MAX_VALUE;
        for (Map.Entry<TeamColor, List<UUID>> team : teams.entrySet()) {
            int size = team.getValue().size();
            if (size < teamSize && size < fewest) {
                best = team.getKey();
                fewest = size;
            }
        }
        return best;
    }
}
