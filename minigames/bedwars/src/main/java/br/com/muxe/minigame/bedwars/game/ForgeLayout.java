package br.com.muxe.minigame.bedwars.game;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

public record ForgeLayout(Shape shape, Facing facing, List<Cell> ironCells) {

    private static final List<Cell> PLUS = List.of(
        new Cell(0, -1), new Cell(-1, 0), new Cell(0, 0), new Cell(1, 0), new Cell(0, 1)
    );
    private static final List<Cell> FRONT_TWO_ROWS = List.of(
        new Cell(-1, 0), new Cell(1, 0), new Cell(0, 0), new Cell(0, 1),
        new Cell(1, 0), new Cell(0, 1), new Cell(-1, 0), new Cell(0, 1)
    );
    private static final List<Cell> FIVE_BY_FIVE_DROPS = List.of(
        new Cell(0, 0), new Cell(1, 2), new Cell(2, 2), new Cell(0, -2), new Cell(1, 0), new Cell(1, -2),
        new Cell(2, 0), new Cell(1, -1), new Cell(2, 1), new Cell(2, -2), new Cell(2, -1), new Cell(-2, 1),
        new Cell(-2, 2), new Cell(-1, 2), new Cell(-2, -1), new Cell(-2, 0), new Cell(-1, 0), new Cell(0, 2),
        new Cell(-1, 1), new Cell(-2, -2), new Cell(0, 1), new Cell(-1, -2), new Cell(-1, -1), new Cell(0, -1)
    );
    public static final int FIVE_BY_FIVE_RADIUS = 2;
    private static final ForgeLayout SINGLE = new ForgeLayout(Shape.SINGLE, Facing.SOUTH, List.of(new Cell(0, 0)));

    public ForgeLayout {
        ironCells = List.copyOf(ironCells);
    }

    public static ForgeLayout single() {
        return SINGLE;
    }

    public static ForgeLayout fiveByFive(double towardX, double towardZ) {
        Facing facing = Facing.closest(towardX, towardZ);
        return new ForgeLayout(Shape.FIVE_BY_FIVE, facing, toWorld(FIVE_BY_FIVE_DROPS, facing));
    }

    public static ForgeLayout detect(Blocks blocks, double towardX, double towardZ) {
        OptionalInt floor = floorY(blocks);
        if (floor.isEmpty()) {
            return SINGLE;
        }
        int y = floor.getAsInt();
        Facing preferred = Facing.closest(towardX, towardZ);
        if (covered(blocks, y, preferred, -1, 1, -1, 1)) {
            return new ForgeLayout(Shape.THREE_BY_THREE, preferred, toWorld(PLUS, preferred));
        }
        for (Facing facing : preferred.withOthers()) {
            if (covered(blocks, y, facing, -1, 1, 0, 1)) {
                return new ForgeLayout(Shape.TWO_BY_THREE, facing, toWorld(FRONT_TWO_ROWS, facing));
            }
        }
        return SINGLE;
    }

    public Cell cellAt(int drop) {
        return ironCells.get(Math.floorMod(drop, ironCells.size()));
    }

    public int cellIndexAt(int drop) {
        return Math.floorMod(drop, ironCells.size());
    }

    private static OptionalInt floorY(Blocks blocks) {
        for (int y = 1; y >= -2; y--) {
            if (isFloor(blocks, 0, y, 0)) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    private static boolean covered(Blocks blocks, int y, Facing forward, int minSide, int maxSide, int minAhead, int maxAhead) {
        for (int side = minSide; side <= maxSide; side++) {
            for (int ahead = minAhead; ahead <= maxAhead; ahead++) {
                Cell cell = forward.toWorld(side, ahead);
                if (!isFloor(blocks, cell.dx(), y, cell.dz())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isFloor(Blocks blocks, int dx, int dy, int dz) {
        return blocks.slab(dx, dy, dz) && !blocks.solid(dx, dy + 1, dz);
    }

    private static List<Cell> toWorld(List<Cell> local, Facing forward) {
        return local.stream().map(cell -> forward.toWorld(cell.dx(), cell.dz())).toList();
    }

    public enum Shape {
        FIVE_BY_FIVE,
        THREE_BY_THREE,
        TWO_BY_THREE,
        SINGLE
    }

    public enum Facing {
        NORTH(0, -1),
        EAST(1, 0),
        SOUTH(0, 1),
        WEST(-1, 0);

        private final int dx;
        private final int dz;

        Facing(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public static Facing closest(double x, double z) {
            if (Math.abs(x) > Math.abs(z)) {
                return x >= 0 ? EAST : WEST;
            }
            return z >= 0 ? SOUTH : NORTH;
        }

        public Facing right() {
            return values()[(ordinal() + 1) % values().length];
        }

        Cell toWorld(int side, int ahead) {
            Facing right = right();
            return new Cell(right.dx * side + dx * ahead, right.dz * side + dz * ahead);
        }

        List<Facing> withOthers() {
            return Stream.concat(Stream.of(this), Arrays.stream(values()).filter(other -> other != this)).toList();
        }
    }

    public record Cell(int dx, int dz) {
    }

    public interface Blocks {

        boolean slab(int dx, int dy, int dz);

        boolean solid(int dx, int dy, int dz);
    }
}
