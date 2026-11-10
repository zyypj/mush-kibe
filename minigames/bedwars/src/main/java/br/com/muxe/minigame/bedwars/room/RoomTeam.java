package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.EnumMap;
import java.util.Map;

public final class RoomTeam {

    private final TeamColor color;
    private final Map<Upgrade, Integer> upgrades = new EnumMap<>(Upgrade.class);

    public RoomTeam(TeamColor color) {
        this.color = color;
    }

    public TeamColor color() {
        return color;
    }

    public int level(Upgrade upgrade) {
        return upgrades.getOrDefault(upgrade, 0);
    }

    public void upgrade(Upgrade upgrade, int level) {
        upgrades.put(upgrade, level);
    }

    public enum Upgrade {
        SHARPNESS,
        PROTECTION,
        HASTE,
        FORGE,
        HEAL_POOL,
        TRAP_BLINDNESS,
        TRAP_FATIGUE,
        BED_PROTECTION
    }
}
