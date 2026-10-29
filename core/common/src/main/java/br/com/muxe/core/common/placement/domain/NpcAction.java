package br.com.muxe.core.common.placement.domain;

import java.util.Locale;
import java.util.Objects;

public record NpcAction(Type type, String target) {

    public static final String SHOP_WORD = "loja";
    public static final String NONE_WORD = "nenhuma";
    public static final NpcAction NONE = new NpcAction(Type.NONE, "");
    public static final NpcAction SHOP = new NpcAction(Type.SHOP, "");

    public enum Type {
        NONE,
        GAME,
        SHOP
    }

    public NpcAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        if (type == Type.GAME && target.isBlank()) {
            throw new IllegalArgumentException("A game NPC needs the group it sends players to");
        }
        if (type != Type.GAME) {
            target = "";
        }
    }

    public static NpcAction game(String group) {
        return new NpcAction(Type.GAME, group);
    }

    public static NpcAction parse(String word) {
        String value = word.toLowerCase(Locale.ROOT);
        return switch (value) {
            case SHOP_WORD -> SHOP;
            case NONE_WORD -> NONE;
            default -> game(value);
        };
    }

    public String describe() {
        return switch (type) {
            case NONE -> NONE_WORD;
            case SHOP -> SHOP_WORD;
            case GAME -> target;
        };
    }
}
