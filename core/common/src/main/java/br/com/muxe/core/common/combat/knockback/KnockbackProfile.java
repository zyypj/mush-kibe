package br.com.muxe.core.common.combat.knockback;

import java.util.Objects;

public record KnockbackProfile(
    double vertical,
    double horizontal,
    double verticalBoost,
    double horizontalBoost,
    double verticalLimit,
    double verticalFriction,
    double horizontalFriction,
    double attackerMultiplier,
    boolean restoreVelocity,
    boolean simulateY,
    boolean keepSprint,
    boolean ignoreSprint,
    boolean useRotation,
    ClickLimiter clickLimiter
) {

    public static final KnockbackProfile VANILLA = new KnockbackProfile(0.4, 0.4, 0.1, 0.5, 0.4, 2.0, 2.0, 0.6,
        true, false, false, false, false, ClickLimiter.OFF);

    public KnockbackProfile {
        requireFinite(vertical, "vertical");
        requireFinite(horizontal, "horizontal");
        requireFinite(verticalBoost, "vertical boost");
        requireFinite(horizontalBoost, "horizontal boost");
        requireFinite(verticalLimit, "vertical limit");
        requireFinite(attackerMultiplier, "attacker multiplier");
        if (!(verticalFriction > 0) || !(horizontalFriction > 0) || Double.isInfinite(verticalFriction) || Double.isInfinite(horizontalFriction)) {
            throw new IllegalArgumentException("Knockback frictions must be positive, got " + verticalFriction + " and " + horizontalFriction);
        }
        Objects.requireNonNull(clickLimiter, "clickLimiter");
    }

    public KnockbackProfile with(KnockbackSetting setting, String value) {
        return setting.write(this, value);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Knockback " + name + " must be a finite number, got " + value);
        }
    }
}
