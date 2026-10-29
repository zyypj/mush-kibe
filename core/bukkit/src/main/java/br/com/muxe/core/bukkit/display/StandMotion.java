package br.com.muxe.core.bukkit.display;

public record StandMotion(double degreesPerTick, double bobHeight, int bobPeriodTicks, double phaseDegrees) {

    public static final StandMotion NONE = new StandMotion(0, 0, 0, 0);

    public StandMotion {
        if (bobHeight < 0) {
            throw new IllegalArgumentException("The bob height cannot be negative, got " + bobHeight);
        }
        if (bobHeight > 0 && bobPeriodTicks <= 0) {
            throw new IllegalArgumentException("A bobbing stand needs a positive period, got " + bobPeriodTicks);
        }
    }

    public static StandMotion spinning(double degreesPerTick) {
        return new StandMotion(degreesPerTick, 0, 0, 0);
    }

    public boolean moves() {
        return degreesPerTick != 0 || bobHeight > 0;
    }

    public float yaw(float baseYaw, long tick) {
        double yaw = baseYaw + degreesPerTick * tick;
        return (float) (((yaw % 360) + 360) % 360);
    }

    public double lift(long tick) {
        if (bobHeight == 0) {
            return 0;
        }
        double angle = 2 * Math.PI * tick / bobPeriodTicks + Math.toRadians(phaseDegrees);
        return bobHeight * Math.sin(angle);
    }
}
