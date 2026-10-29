package br.com.muxe.core.common.placement.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record HologramPlacement(String group, String id, Position position, List<String> lines) implements Placement {

    public HologramPlacement {
        PlacementIds.requireGroup(group);
        PlacementIds.require(id);
        Objects.requireNonNull(position, "position");
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("A hologram needs at least one line");
        }
    }

    @Override
    public PlacementKind kind() {
        return PlacementKind.HOLOGRAM;
    }

    public HologramPlacement movedTo(Position target) {
        return new HologramPlacement(group, id, target, lines);
    }

    public HologramPlacement adding(String line) {
        List<String> changed = new ArrayList<>(lines);
        changed.add(line);
        return new HologramPlacement(group, id, position, changed);
    }

    public HologramPlacement replacing(int number, String line) {
        List<String> changed = new ArrayList<>(lines);
        changed.set(index(number), line);
        return new HologramPlacement(group, id, position, changed);
    }

    public HologramPlacement without(int number) {
        List<String> changed = new ArrayList<>(lines);
        changed.remove(index(number));
        return new HologramPlacement(group, id, position, changed);
    }

    public boolean hasLine(int number) {
        return number >= 1 && number <= lines.size();
    }

    private int index(int number) {
        if (!hasLine(number)) {
            throw new IllegalArgumentException("Line " + number + " does not exist, the hologram has " + lines.size());
        }
        return number - 1;
    }
}
