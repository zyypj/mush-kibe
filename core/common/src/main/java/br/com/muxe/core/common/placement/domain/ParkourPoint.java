package br.com.muxe.core.common.placement.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record ParkourPoint(Type type, int number) {

    public static final String START_ID = "start";
    public static final String END_ID = "end";
    public static final String CHECKPOINT_PREFIX = "checkpoint-";

    public enum Type {
        START,
        CHECKPOINT,
        END
    }

    public ParkourPoint {
        Objects.requireNonNull(type, "type");
        if (type == Type.CHECKPOINT && number < 1) {
            throw new IllegalArgumentException("Checkpoints are numbered from 1, got: " + number);
        }
        if (type != Type.CHECKPOINT) {
            number = 0;
        }
    }

    public static ParkourPoint start() {
        return new ParkourPoint(Type.START, 0);
    }

    public static ParkourPoint end() {
        return new ParkourPoint(Type.END, 0);
    }

    public static ParkourPoint checkpoint(int number) {
        return new ParkourPoint(Type.CHECKPOINT, number);
    }

    public static Optional<ParkourPoint> parse(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.equals(START_ID)) {
            return Optional.of(start());
        }
        if (lower.equals(END_ID)) {
            return Optional.of(end());
        }
        if (!lower.startsWith(CHECKPOINT_PREFIX)) {
            return Optional.empty();
        }
        try {
            int number = Integer.parseInt(lower.substring(CHECKPOINT_PREFIX.length()));
            return number < 1 ? Optional.empty() : Optional.of(checkpoint(number));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public String id() {
        return switch (type) {
            case START -> START_ID;
            case END -> END_ID;
            case CHECKPOINT -> CHECKPOINT_PREFIX + number;
        };
    }
}
