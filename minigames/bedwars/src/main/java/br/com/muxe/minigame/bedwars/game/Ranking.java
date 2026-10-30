package br.com.muxe.minigame.bedwars.game;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Ranking {

    public static final int TOP = 3;

    private Ranking() {
    }

    public static List<Entry> top(Map<UUID, Integer> scores, List<UUID> order) {
        return scores.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .sorted(Comparator.comparing((Map.Entry<UUID, Integer> entry) -> entry.getValue()).reversed()
                .thenComparingInt(entry -> position(order, entry.getKey())))
            .limit(TOP)
            .map(entry -> new Entry(entry.getKey(), entry.getValue()))
            .toList();
    }

    private static int position(List<UUID> order, UUID player) {
        int index = order.indexOf(player);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public record Entry(UUID player, int score) {
    }
}
