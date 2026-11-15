package br.com.muxe.core.common.stats.domain;

public final class BedWarsLevels {

    public static final long BASE = 5_000;
    public static final int LEVELS_PER_STEP = 10;
    public static final int FIRST_LEVEL = 1;

    private BedWarsLevels() {
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
        int level = FIRST_LEVEL;
        long left = totalXp;
        while (left >= costOf(level)) {
            left -= costOf(level);
            level++;
        }
        return new Progress(level, left, costOf(level));
    }

    public record Progress(int level, long xp, long needed) {

        public static final float FULLEST_BAR = 0.99f;

        public float fill() {
            if (needed <= 0) {
                return 0f;
            }
            return Math.min(FULLEST_BAR, Math.max(0f, (float) xp / needed));
        }

        public String compactNeeded() {
            return needed % 1000 == 0 ? needed / 1000 + "k" : String.valueOf(needed);
        }
    }
}
