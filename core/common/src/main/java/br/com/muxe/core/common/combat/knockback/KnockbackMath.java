package br.com.muxe.core.common.combat.knockback;

public final class KnockbackMath {

    public static final double GRAVITY = 0.08;
    public static final double AIR_DRAG = 0.98;
    public static final double AIR_SLIP = 0.91;
    public static final double GROUND_SLIP = 0.546;
    public static final double RESTING_Y = -0.0784;
    public static final double TERMINAL_Y = -3.92;
    public static final int MAX_SIMULATED_TICKS = 40;
    public static final int TICK_MILLIS = 50;

    private static final double RADIANS = Math.PI / 180.0;
    private static final double MIN_DISTANCE_SQUARED = 1.0E-4;

    private KnockbackMath() {
    }

    public static void push(KnockbackProfile profile, Motion previous, double towardsAttackerX, double towardsAttackerZ, float attackerYaw,
                            int level, boolean base, Motion out) {
        double x = previous.x();
        double y = previous.y();
        double z = previous.z();
        double yaw = attackerYaw * RADIANS;
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        if (base) {
            double dx = towardsAttackerX;
            double dz = towardsAttackerZ;
            if (profile.useRotation() || dx * dx + dz * dz < MIN_DISTANCE_SQUARED) {
                dx = sin;
                dz = -cos;
            }
            double distance = Math.sqrt(dx * dx + dz * dz);
            x = x / profile.horizontalFriction() - dx / distance * profile.horizontal();
            y = y / profile.verticalFriction() + profile.vertical();
            z = z / profile.horizontalFriction() - dz / distance * profile.horizontal();
            if (y > profile.verticalLimit()) {
                y = profile.verticalLimit();
            }
        }
        if (level > 0) {
            x -= sin * level * profile.horizontalBoost();
            y += profile.verticalBoost();
            z += cos * level * profile.horizontalBoost();
        }
        out.set(x, y, z);
    }

    public static double fall(double motionY, int ticks) {
        double y = motionY;
        int steps = Math.min(Math.max(ticks, 0), MAX_SIMULATED_TICKS);
        for (int step = 0; step < steps && y > TERMINAL_Y; step++) {
            y = (y - GRAVITY) * AIR_DRAG;
        }
        return Math.max(y, TERMINAL_Y);
    }

    public static double slide(int ticks, boolean onGround) {
        if (ticks <= 0) {
            return 1.0;
        }
        return Math.pow(onGround ? GROUND_SLIP : AIR_SLIP, Math.min(ticks, MAX_SIMULATED_TICKS));
    }

    public static int pingTicks(int pingMillis) {
        return (Math.max(pingMillis, 0) + TICK_MILLIS - 1) / TICK_MILLIS;
    }
}
