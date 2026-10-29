package br.com.muxe.core.bukkit.menu;

import java.util.List;

public record Page<T>(List<T> items, int number, int total) {

    public Page {
        items = List.copyOf(items);
    }

    public static <T> Page<T> of(List<T> all, int size, int requested) {
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        int total = Math.max(1, (all.size() + size - 1) / size);
        int number = Math.min(Math.max(1, requested), total);
        int from = Math.min(all.size(), (number - 1) * size);
        int to = Math.min(all.size(), from + size);
        return new Page<>(all.subList(from, to), number, total);
    }

    public boolean hasPrevious() {
        return number > 1;
    }

    public boolean hasNext() {
        return number < total;
    }
}
