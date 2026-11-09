package br.com.muxe.core.common.customroom.schema;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record OptionPage(String id, String title, int rows, int backSlot, String backTo, List<OptionEntry> entries, Optional<Turn> previous, Optional<Turn> next) {

    public OptionPage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        backTo = backTo == null ? "" : backTo;
        entries = List.copyOf(entries);
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Page " + id + " needs 1 to 6 rows, got " + rows);
        }
    }

    public Optional<OptionEntry> at(int slot) {
        return entries.stream().filter(entry -> entry.slot() == slot).findFirst();
    }

    public record Turn(int slot, String page, int number) {
    }
}
