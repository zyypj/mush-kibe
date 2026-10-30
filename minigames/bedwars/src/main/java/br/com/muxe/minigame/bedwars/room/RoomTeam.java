package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RoomTeam {

    private final TeamColor color;
    private final List<UUID> members = new ArrayList<>();
    private final Map<Upgrade, Integer> upgrades = new EnumMap<>(Upgrade.class);
    private boolean bedAlive = true;
    private UUID bedBreaker;

    public RoomTeam(TeamColor color) {
        this.color = color;
    }

    public TeamColor color() {
        return color;
    }

    public List<UUID> members() {
        return Collections.unmodifiableList(members);
    }

    void add(UUID member) {
        members.add(member);
    }

    public boolean bedAlive() {
        return bedAlive;
    }

    public Optional<UUID> bedBreaker() {
        return Optional.ofNullable(bedBreaker);
    }

    void breakBed(UUID breaker) {
        bedAlive = false;
        bedBreaker = breaker;
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
        TRAP_FATIGUE
    }
}
