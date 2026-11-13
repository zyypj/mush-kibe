package br.com.muxe.minigame.bedwars.shop;

import java.util.ArrayList;
import java.util.List;

public final class ShopLayout {

    public static final int FIRST_SLOT = 19;
    public static final int COLUMNS = 7;
    public static final int ROWS = 3;
    public static final int ROW_WIDTH = 9;

    private ShopLayout() {
    }

    public static List<Integer> centered(int count) {
        int shown = Math.min(count, COLUMNS * ROWS);
        int rows = (shown + COLUMNS - 1) / COLUMNS;
        int firstRow = (ROWS - rows) / 2;
        List<Integer> slots = new ArrayList<>(shown);
        for (int row = 0; row < rows; row++) {
            int inRow = Math.min(COLUMNS, shown - row * COLUMNS);
            for (int column : columns(inRow)) {
                slots.add(FIRST_SLOT + (firstRow + row) * ROW_WIDTH + column);
            }
        }
        return slots;
    }

    static List<Integer> columns(int count) {
        int middle = COLUMNS / 2;
        List<Integer> columns = new ArrayList<>(count);
        if (count % 2 == 1) {
            for (int column = middle - count / 2; column <= middle + count / 2; column++) {
                columns.add(column);
            }
            return columns;
        }
        for (int column = middle - count / 2; column < middle; column++) {
            columns.add(column);
        }
        for (int column = middle + 1; column <= middle + count / 2; column++) {
            columns.add(column);
        }
        return columns;
    }
}
