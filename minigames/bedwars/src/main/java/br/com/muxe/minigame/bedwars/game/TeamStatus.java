package br.com.muxe.minigame.bedwars.game;

public record TeamStatus(Kind kind, int alive) {

    public enum Kind {
        BED_ONLINE,
        BED_OFFLINE,
        NO_BED,
        ELIMINATED
    }

    public static TeamStatus of(boolean bedAlive, int aliveMembers, int connectedMembers, boolean canReturn) {
        if (aliveMembers <= 0 && !(bedAlive && canReturn)) {
            return new TeamStatus(Kind.ELIMINATED, 0);
        }
        if (!bedAlive) {
            return new TeamStatus(Kind.NO_BED, aliveMembers);
        }
        return new TeamStatus(connectedMembers > 0 ? Kind.BED_ONLINE : Kind.BED_OFFLINE, aliveMembers);
    }

    public boolean isEliminated() {
        return kind == Kind.ELIMINATED;
    }
}
