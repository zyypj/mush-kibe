package br.com.muxe.minigame.engine.setup;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record SetupStep(String id, String label, StepKind kind, boolean perTeam, int minimum, int maximum, List<String> hologram) {

    private static final Pattern ID = Pattern.compile("[a-z0-9-]+");

    public SetupStep {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Setup step id must match " + ID.pattern() + ", got: " + id);
        }
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(kind, "kind");
        if (kind == StepKind.REGION) {
            minimum = Math.min(minimum, 2);
            maximum = 2;
        }
        if (kind == StepKind.HEIGHT) {
            maximum = 1;
        }
        if (minimum < 0 || maximum < 1 || maximum < minimum) {
            throw new IllegalArgumentException("Invalid limits for step " + id + ": " + minimum + "-" + maximum);
        }
        hologram = List.copyOf(hologram);
    }

    public static SetupStep single(String id, String label, StepKind kind, boolean perTeam, String... hologram) {
        return new SetupStep(id, label, kind, perTeam, 1, 1, List.of(hologram));
    }

    public static SetupStep many(String id, String label, StepKind kind, boolean perTeam, int minimum, int maximum, String... hologram) {
        return new SetupStep(id, label, kind, perTeam, minimum, maximum, List.of(hologram));
    }

    public static SetupStep optional(String id, String label, StepKind kind, boolean perTeam, String... hologram) {
        return new SetupStep(id, label, kind, perTeam, 0, 1, List.of(hologram));
    }

    public boolean isRequired() {
        return minimum > 0;
    }

    public boolean isList() {
        return maximum > 1 && kind != StepKind.REGION;
    }
}
