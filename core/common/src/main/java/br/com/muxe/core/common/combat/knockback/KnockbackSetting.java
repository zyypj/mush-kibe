package br.com.muxe.core.common.combat.knockback;

import java.util.Locale;
import java.util.Optional;

public enum KnockbackSetting {
    VERTICAL("vertical", Kind.DECIMAL),
    HORIZONTAL("horizontal", Kind.DECIMAL),
    VERTICAL_BOOST("vertical-boost", Kind.DECIMAL),
    HORIZONTAL_BOOST("horizontal-boost", Kind.DECIMAL),
    VERTICAL_LIMIT("vertical-limit", Kind.DECIMAL),
    VERTICAL_FRICTION("vertical-friction", Kind.DECIMAL),
    HORIZONTAL_FRICTION("horizontal-friction", Kind.DECIMAL),
    ATTACKER_MULTIPLIER("attacker-multiplier", Kind.DECIMAL),
    RESTORE_VELOCITY("restore-velocity", Kind.TOGGLE),
    SIMULATE_Y("simulate-y", Kind.TOGGLE),
    KEEP_SPRINT("keep-sprint", Kind.TOGGLE),
    IGNORE_SPRINT("ignore-sprint", Kind.TOGGLE),
    USE_ROTATION("use-rotation", Kind.TOGGLE),
    CLICK_LIMITER("click-limiter", Kind.CHOICE);

    private final String key;
    private final Kind kind;

    KnockbackSetting(String key, Kind kind) {
        this.key = key;
        this.kind = kind;
    }

    public String key() {
        return key;
    }

    public Kind kind() {
        return kind;
    }

    public static Optional<KnockbackSetting> byKey(String key) {
        String wanted = key.toLowerCase(Locale.ROOT);
        for (KnockbackSetting setting : values()) {
            if (setting.key.equals(wanted)) {
                return Optional.of(setting);
            }
        }
        return Optional.empty();
    }

    public String read(KnockbackProfile profile) {
        return switch (this) {
            case VERTICAL -> number(profile.vertical());
            case HORIZONTAL -> number(profile.horizontal());
            case VERTICAL_BOOST -> number(profile.verticalBoost());
            case HORIZONTAL_BOOST -> number(profile.horizontalBoost());
            case VERTICAL_LIMIT -> number(profile.verticalLimit());
            case VERTICAL_FRICTION -> number(profile.verticalFriction());
            case HORIZONTAL_FRICTION -> number(profile.horizontalFriction());
            case ATTACKER_MULTIPLIER -> number(profile.attackerMultiplier());
            case RESTORE_VELOCITY -> String.valueOf(profile.restoreVelocity());
            case SIMULATE_Y -> String.valueOf(profile.simulateY());
            case KEEP_SPRINT -> String.valueOf(profile.keepSprint());
            case IGNORE_SPRINT -> String.valueOf(profile.ignoreSprint());
            case USE_ROTATION -> String.valueOf(profile.useRotation());
            case CLICK_LIMITER -> profile.clickLimiter().key();
        };
    }

    public KnockbackProfile write(KnockbackProfile profile, String value) {
        Values values = new Values(profile);
        switch (this) {
            case VERTICAL -> values.vertical = decimal(value);
            case HORIZONTAL -> values.horizontal = decimal(value);
            case VERTICAL_BOOST -> values.verticalBoost = decimal(value);
            case HORIZONTAL_BOOST -> values.horizontalBoost = decimal(value);
            case VERTICAL_LIMIT -> values.verticalLimit = decimal(value);
            case VERTICAL_FRICTION -> values.verticalFriction = decimal(value);
            case HORIZONTAL_FRICTION -> values.horizontalFriction = decimal(value);
            case ATTACKER_MULTIPLIER -> values.attackerMultiplier = decimal(value);
            case RESTORE_VELOCITY -> values.restoreVelocity = toggle(value);
            case SIMULATE_Y -> values.simulateY = toggle(value);
            case KEEP_SPRINT -> values.keepSprint = toggle(value);
            case IGNORE_SPRINT -> values.ignoreSprint = toggle(value);
            case USE_ROTATION -> values.useRotation = toggle(value);
            case CLICK_LIMITER -> values.clickLimiter = ClickLimiter.parse(value)
                .orElseThrow(() -> new IllegalArgumentException("Unknown click limiter: " + value));
        }
        return values.build();
    }

    public static String number(double value) {
        return Double.toString(value);
    }

    public static double decimal(String value) {
        try {
            double parsed = Double.parseDouble(value.trim().replace(',', '.'));
            if (!Double.isFinite(parsed)) {
                throw new IllegalArgumentException("Not a finite number: " + value);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Not a number: " + value, exception);
        }
    }

    private static boolean toggle(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Not true or false: " + value);
        };
    }

    public enum Kind {
        DECIMAL,
        TOGGLE,
        CHOICE
    }

    private static final class Values {

        private double vertical;
        private double horizontal;
        private double verticalBoost;
        private double horizontalBoost;
        private double verticalLimit;
        private double verticalFriction;
        private double horizontalFriction;
        private double attackerMultiplier;
        private boolean restoreVelocity;
        private boolean simulateY;
        private boolean keepSprint;
        private boolean ignoreSprint;
        private boolean useRotation;
        private ClickLimiter clickLimiter;

        private Values(KnockbackProfile profile) {
            vertical = profile.vertical();
            horizontal = profile.horizontal();
            verticalBoost = profile.verticalBoost();
            horizontalBoost = profile.horizontalBoost();
            verticalLimit = profile.verticalLimit();
            verticalFriction = profile.verticalFriction();
            horizontalFriction = profile.horizontalFriction();
            attackerMultiplier = profile.attackerMultiplier();
            restoreVelocity = profile.restoreVelocity();
            simulateY = profile.simulateY();
            keepSprint = profile.keepSprint();
            ignoreSprint = profile.ignoreSprint();
            useRotation = profile.useRotation();
            clickLimiter = profile.clickLimiter();
        }

        private KnockbackProfile build() {
            return new KnockbackProfile(vertical, horizontal, verticalBoost, horizontalBoost, verticalLimit, verticalFriction, horizontalFriction,
                attackerMultiplier, restoreVelocity, simulateY, keepSprint, ignoreSprint, useRotation, clickLimiter);
        }
    }
}
