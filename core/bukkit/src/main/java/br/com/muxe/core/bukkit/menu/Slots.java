package br.com.muxe.core.bukkit.menu;

import java.util.ArrayList;
import java.util.List;

public final class Slots {

    private Slots() {
    }

    public static int of(int row, int column) {
        if (row < 0 || row >= Menu.MAX_ROWS || column < 0 || column >= MenuLayout.ROW_WIDTH) {
            throw new IllegalArgumentException("Rows go from 0 to 5 and columns from 0 to 8, got row " + row + " column " + column);
        }
        return row * MenuLayout.ROW_WIDTH + column;
    }

    public static List<Integer> row(int row) {
        List<Integer> slots = new ArrayList<>(MenuLayout.ROW_WIDTH);
        for (int column = 0; column < MenuLayout.ROW_WIDTH; column++) {
            slots.add(of(row, column));
        }
        return slots;
    }

    public static List<Integer> border(int rows) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < MenuLayout.ROW_WIDTH; column++) {
                if (row == 0 || row == rows - 1 || column == 0 || column == MenuLayout.ROW_WIDTH - 1) {
                    slots.add(of(row, column));
                }
            }
        }
        return slots;
    }
}
