package br.com.muxe.minigame.bedwars.game;

import br.com.muxe.minigame.bedwars.game.ForgeLayout.Cell;
import br.com.muxe.minigame.bedwars.game.ForgeLayout.Facing;
import java.time.Duration;
import java.util.List;

public final class CompactTower {

    public static final String ITEM_ID = "torre-compacta";
    public static final Duration COOLDOWN = Duration.ofSeconds(15);
    public static final float SOUND_PITCH = 1.49f;
    public static final List<Piece> PIECES = List.of(
        new Piece(0, 1, 0, -1, false), new Piece(0, -1, 0, -1, false), new Piece(0, 2, 0, 0, false), new Piece(0, -2, 0, 0, false),
        new Piece(0, 2, 0, 1, false), new Piece(0, 0, 0, 1, true), new Piece(0, -2, 0, 1, false), new Piece(0, 1, 0, 2, false),
        new Piece(0, 0, 0, 2, false), new Piece(0, -1, 0, 2, false), new Piece(2, 1, 1, -1, false), new Piece(2, -1, 1, -1, false),
        new Piece(2, 2, 1, 0, false), new Piece(2, -2, 1, 0, false), new Piece(2, 2, 1, 1, false), new Piece(2, 0, 1, 1, true),
        new Piece(2, -2, 1, 1, false), new Piece(2, 1, 1, 2, false), new Piece(2, 0, 1, 2, false), new Piece(2, -1, 1, 2, false),
        new Piece(4, 1, 2, -1, false), new Piece(4, -1, 2, -1, false), new Piece(4, 2, 2, 0, false), new Piece(4, -2, 2, 0, false),
        new Piece(4, 2, 2, 1, false), new Piece(4, 0, 2, 1, true), new Piece(4, -2, 2, 1, false), new Piece(4, 1, 2, 2, false),
        new Piece(4, 0, 2, 2, false), new Piece(4, -1, 2, 2, false), new Piece(6, 1, 3, -1, false), new Piece(6, 0, 3, -1, false),
        new Piece(6, -1, 3, -1, false), new Piece(6, 2, 3, 0, false), new Piece(6, -2, 3, 0, false), new Piece(6, 2, 3, 1, false),
        new Piece(6, 0, 3, 1, true), new Piece(6, -2, 3, 1, false), new Piece(6, 1, 3, 2, false), new Piece(6, 0, 3, 2, false),
        new Piece(7, -1, 3, 2, false), new Piece(7, 1, 4, -1, false), new Piece(7, 0, 4, -1, false), new Piece(7, -1, 4, -1, false),
        new Piece(7, 2, 4, 0, false), new Piece(8, -2, 4, 0, false), new Piece(8, 2, 4, 1, false), new Piece(8, 0, 4, 1, true),
        new Piece(8, -2, 4, 1, false), new Piece(8, 1, 4, 2, false), new Piece(9, 0, 4, 2, false), new Piece(9, -1, 4, 2, false),
        new Piece(9, 2, 5, -2, false), new Piece(9, 0, 5, -2, false), new Piece(9, -2, 5, -2, false), new Piece(11, 3, 5, -1, false),
        new Piece(11, 2, 5, -1, false), new Piece(11, 1, 5, -1, false), new Piece(11, 0, 5, -1, false), new Piece(11, -1, 5, -1, false),
        new Piece(11, -2, 5, -1, false), new Piece(11, 2, 5, 0, false), new Piece(11, 1, 5, 0, false), new Piece(11, 0, 5, 0, false),
        new Piece(11, -3, 5, -1, false), new Piece(12, -1, 5, 0, false), new Piece(12, -2, 5, 0, false), new Piece(12, 2, 5, 1, false),
        new Piece(12, 1, 5, 1, false), new Piece(12, 0, 5, 1, true), new Piece(13, -1, 5, 1, false), new Piece(13, -2, 5, 1, false),
        new Piece(13, 3, 5, 2, false), new Piece(13, 2, 5, 2, false), new Piece(13, 1, 5, 2, false), new Piece(14, 2, 5, 3, false),
        new Piece(14, 0, 5, 2, false), new Piece(14, -1, 5, 2, false), new Piece(14, -2, 5, 2, false), new Piece(14, -3, 5, 2, false),
        new Piece(15, 0, 5, 3, false), new Piece(15, -2, 5, 3, false), new Piece(15, 2, 6, -2, false), new Piece(15, 1, 6, -2, false),
        new Piece(15, 0, 6, -2, false), new Piece(16, -1, 6, -2, false), new Piece(16, -2, 6, -2, false), new Piece(16, 3, 6, -1, false),
        new Piece(16, 3, 6, 0, false), new Piece(16, -3, 6, -1, false), new Piece(17, 3, 6, 1, false), new Piece(17, 3, 6, 2, false),
        new Piece(17, -3, 6, 0, false), new Piece(17, -3, 6, 1, false), new Piece(17, -3, 6, 2, false), new Piece(19, 2, 6, 3, false),
        new Piece(19, 1, 6, 3, false), new Piece(19, 0, 6, 3, false), new Piece(19, -1, 6, 3, false), new Piece(19, -2, 6, 3, false),
        new Piece(19, 2, 7, -2, false), new Piece(19, 0, 7, -2, false), new Piece(19, -2, 7, -2, false), new Piece(19, 3, 7, -1, false),
        new Piece(19, -3, 7, -1, false), new Piece(21, 2, 7, 3, false), new Piece(21, 0, 7, 3, false), new Piece(21, -2, 7, 3, false),
        new Piece(21, 3, 7, 2, false), new Piece(21, -3, 7, 2, false)
    );
    public static final int LAST_TICK = PIECES.stream().mapToInt(Piece::tick).max().orElse(0);

    private CompactTower() {
    }

    public static List<Placed> at(int tick, int x, int y, int z, Facing facing) {
        return PIECES.stream().filter(piece -> piece.tick() == tick).map(piece -> {
            Cell cell = facing.toWorld(piece.side(), piece.ahead());
            return new Placed(x + cell.dx(), y + piece.up(), z + cell.dz(), piece.ladder());
        }).toList();
    }

    public static byte ladderData(Facing facing) {
        return switch (facing) {
            case EAST -> 4;
            case WEST -> 5;
            case SOUTH -> 2;
            case NORTH -> 3;
        };
    }

    public static Facing facing(float yaw) {
        double radians = Math.toRadians(yaw);
        return Facing.closest(-Math.sin(radians), Math.cos(radians));
    }

    public record Piece(int tick, int side, int up, int ahead, boolean ladder) {
    }

    public record Placed(int x, int y, int z, boolean ladder) {
    }
}
