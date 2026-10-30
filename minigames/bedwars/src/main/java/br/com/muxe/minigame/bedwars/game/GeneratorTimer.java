package br.com.muxe.minigame.bedwars.game;

public final class GeneratorTimer {

    public static final int CYCLES_PER_SECOND = 4;
    public static final long TICKS_PER_CYCLE = 5L;

    private double delayCycles;
    private int amount;
    private int spawnLimit;
    private double remaining;

    public GeneratorTimer(Rate rate) {
        apply(rate);
        this.remaining = delayCycles;
    }

    public void apply(Rate rate) {
        this.delayCycles = rate.delaySeconds() * CYCLES_PER_SECOND;
        this.amount = rate.amount();
        this.spawnLimit = rate.spawnLimit();
    }

    public int cycle(int grounded) {
        if (remaining <= 0) {
            remaining = delayCycles - 1;
            if (spawnLimit <= 0) {
                return amount;
            }
            return Math.max(0, Math.min(amount, spawnLimit - grounded));
        }
        remaining--;
        return 0;
    }

    public int secondsLeft() {
        return (int) Math.ceil(Math.max(0, remaining) / CYCLES_PER_SECOND);
    }

    public int amount() {
        return amount;
    }

    public record Rate(double delaySeconds, int amount, int spawnLimit) {

        public Rate {
            if (delaySeconds <= 0 || amount < 1 || spawnLimit < 0) {
                throw new IllegalArgumentException("Invalid generator rate " + delaySeconds + "s x" + amount + " limit " + spawnLimit);
            }
        }
    }
}
