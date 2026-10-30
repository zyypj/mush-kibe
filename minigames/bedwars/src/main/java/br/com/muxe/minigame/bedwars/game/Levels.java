package br.com.muxe.minigame.bedwars.game;

public final class Levels {

    public static final long BASE = 5_000;
    public static final int LEVELS_PER_STEP = 10;

    private Levels() {
    }

    public static long costOf(int targetLevel) {
        if (targetLevel < 1) {
            throw new IllegalArgumentException("Level " + targetLevel + " has no cost");
        }
        return BASE * (targetLevel / LEVELS_PER_STEP + 1);
    }

    public static Progress progress(long totalXp) {
        if (totalXp < 0) {
            throw new IllegalArgumentException("Negative XP " + totalXp);
        }
        int level = 0;
        long left = totalXp;
        while (left >= costOf(level + 1)) {
            left -= costOf(level + 1);
            level++;
        }
        return new Progress(level, left, costOf(level + 1));
    }

    public record Progress(int level, long xp, long needed) {

        public String compactNeeded() {
            return needed % 1000 == 0 ? needed / 1000 + "k" : String.valueOf(needed);
        }
    }
}
