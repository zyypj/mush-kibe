package br.com.muxe.minigame.bedwars.game;

public final class FallCap {

    public static final double MAX_DAMAGE = 3.0;
    public static final long GRACE_MILLIS = 500;
    public static final long LIMIT_MILLIS = 15_000;

    private FallCap() {
    }

    public static double damage(double fall, long launchedAt, long now) {
        return now - launchedAt <= LIMIT_MILLIS ? Math.min(fall, MAX_DAMAGE) : fall;
    }

    public static boolean landed(long launchedAt, long now, boolean onGround) {
        return onGround && now - launchedAt >= GRACE_MILLIS;
    }
}
