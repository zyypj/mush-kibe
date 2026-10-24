package br.com.muxe.lobby.visibility.domain;

public enum Visibility {
    ALL,
    FRIENDS,
    NONE;

    public Visibility next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean shows(boolean friends) {
        return switch (this) {
            case ALL -> true;
            case FRIENDS -> friends;
            case NONE -> false;
        };
    }

    public static Visibility parse(String value) {
        for (Visibility visibility : values()) {
            if (visibility.name().equals(value)) {
                return visibility;
            }
        }
        return ALL;
    }
}
