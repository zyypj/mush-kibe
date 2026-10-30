package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.time.Instant;
import java.util.UUID;

public final class RoomPlayer {

    public enum State {
        WAITING,
        ALIVE,
        RESPAWNING,
        DISCONNECTED,
        ELIMINATED
    }

    private final UUID id;
    private final String name;
    private TeamColor team;
    private State state = State.WAITING;
    private int kills;
    private int finalKills;
    private int beds;
    private int respawnAt;
    private Instant protectedUntil = Instant.MIN;
    private Instant disconnectedUntil = Instant.MIN;
    private int armorTier;
    private int pickaxeTier;
    private int axeTier;
    private boolean shears;

    public RoomPlayer(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TeamColor team() {
        return team;
    }

    void team(TeamColor value) {
        this.team = value;
    }

    public State state() {
        return state;
    }

    void state(State value) {
        this.state = value;
    }

    public boolean isAlive() {
        return state == State.ALIVE;
    }

    public boolean isPlaying() {
        return state == State.ALIVE || state == State.RESPAWNING || state == State.DISCONNECTED;
    }

    public int kills() {
        return kills;
    }

    public int finalKills() {
        return finalKills;
    }

    public int beds() {
        return beds;
    }

    void addKill(boolean finalKill) {
        if (finalKill) {
            finalKills++;
        } else {
            kills++;
        }
    }

    void addBed() {
        beds++;
    }

    public int respawnAt() {
        return respawnAt;
    }

    void respawnAt(int elapsedSeconds) {
        this.respawnAt = elapsedSeconds;
    }

    public boolean isProtected(Instant now) {
        return now.isBefore(protectedUntil);
    }

    void protectUntil(Instant until) {
        this.protectedUntil = until;
    }

    public Instant disconnectedUntil() {
        return disconnectedUntil;
    }

    void disconnectedUntil(Instant until) {
        this.disconnectedUntil = until;
    }

    public int armorTier() {
        return armorTier;
    }

    public void armorTier(int tier) {
        this.armorTier = Math.max(armorTier, tier);
    }

    public int pickaxeTier() {
        return pickaxeTier;
    }

    public void pickaxeTier(int tier) {
        this.pickaxeTier = tier;
    }

    public int axeTier() {
        return axeTier;
    }

    public void axeTier(int tier) {
        this.axeTier = tier;
    }

    public boolean hasShears() {
        return shears;
    }

    public void giveShears() {
        this.shears = true;
    }

    void downgradeTools() {
        pickaxeTier = Math.max(pickaxeTier > 0 ? 1 : 0, pickaxeTier - 1);
        axeTier = Math.max(axeTier > 0 ? 1 : 0, axeTier - 1);
    }
}
