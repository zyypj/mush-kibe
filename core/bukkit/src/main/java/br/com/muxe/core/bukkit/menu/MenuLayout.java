package br.com.muxe.core.bukkit.menu;

import java.util.ArrayList;
import java.util.List;

public final class MenuLayout {

    public static final int ROW_WIDTH = 9;
    public static final int ITEMS_PER_ROW = 7;
    public static final int MAX_CONTENT_ROWS = 3;
    public static final int ITEMS_PER_PAGE = ITEMS_PER_ROW * MAX_CONTENT_ROWS;

    private MenuLayout() {
    }

    public static int contentRows(int count) {
        return Math.max(1, (count + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW);
    }

    public static int rowsFor(int count) {
        return contentRows(count) + 2;
    }

    public static List<Integer> centered(int count, int firstRow) {
        List<Integer> slots = new ArrayList<>(count);
        int remaining = count;
        int row = firstRow;
        while (remaining > 0) {
            int inRow = Math.min(ITEMS_PER_ROW, remaining);
            int firstColumn = (ROW_WIDTH - inRow) / 2;
            for (int column = 0; column < inRow; column++) {
                slots.add(row * ROW_WIDTH + firstColumn + column);
            }
            remaining -= inRow;
            row++;
        }
        return slots;
    }
}
