package br.com.muxe.minigame.bedwars.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedDefense {

    private BedDefense() {
    }

    public static List<Layer> layers(int level) {
        return switch (level) {
            case 1 -> List.of(Layer.WOOD, Layer.WOOL, Layer.GLASS);
            case 2 -> List.of(Layer.WOOD, Layer.WOOL, Layer.END_STONE);
            case 3 -> List.of(Layer.WOOD, Layer.WOOL, Layer.END_STONE, Layer.GLASS);
            default -> List.of();
        };
    }

    public static Map<Pos, Layer> plan(List<Pos> bed, int level) {
        List<Layer> layers = layers(level);
        Map<Pos, Layer> plan = new LinkedHashMap<>();
        if (bed.isEmpty() || layers.isEmpty()) {
            return plan;
        }
        int reach = layers.size();
        int minX = bed.stream().mapToInt(Pos::x).min().orElseThrow() - reach;
        int maxX = bed.stream().mapToInt(Pos::x).max().orElseThrow() + reach;
        int minZ = bed.stream().mapToInt(Pos::z).min().orElseThrow() - reach;
        int maxZ = bed.stream().mapToInt(Pos::z).max().orElseThrow() + reach;
        int baseY = bed.stream().mapToInt(Pos::y).min().orElseThrow();
        for (int y = baseY; y <= baseY + reach; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int distance = distance(bed, x, y, z);
                    if (distance >= 1 && distance <= reach) {
                        plan.put(new Pos(x, y, z), layers.get(distance - 1));
                    }
                }
            }
        }
        return plan;
    }

    private static int distance(List<Pos> bed, int x, int y, int z) {
        int best = Integer.MAX_VALUE;
        for (Pos part : bed) {
            best = Math.min(best, Math.abs(part.x() - x) + Math.abs(part.y() - y) + Math.abs(part.z() - z));
        }
        return best;
    }

    public enum Layer {
        WOOD,
        WOOL,
        GLASS,
        END_STONE
    }

    public record Pos(int x, int y, int z) {
    }
}
