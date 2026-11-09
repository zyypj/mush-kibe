package br.com.muxe.core.common.combat.knockback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class KnockbackMathTest {

    private static final KnockbackProfile BEDWARS = new KnockbackProfile(0.6, 0.75, 0.1, 0.5, 0.4, 2.0, 2.0, 0.6,
        false, true, false, true, false, ClickLimiter.NORMAL);

    private final Motion out = new Motion();

    @Test
    void vanillaPushesAwayFromTheAttackerAndCapsTheBaseLift() {
        Motion still = new Motion();

        KnockbackMath.push(KnockbackProfile.VANILLA, still, -3, 0, 90, 0, true, out);

        assertThat(out.x()).isCloseTo(0.4, within(1e-9));
        assertThat(out.y()).isCloseTo(0.4, within(1e-9));
        assertThat(out.z()).isCloseTo(0, within(1e-9));
    }

    @Test
    void thePreviousMotionIsDividedByTheFrictions() {
        Motion previous = new Motion().set(0.2, -0.4, 0.1);

        KnockbackMath.push(KnockbackProfile.VANILLA, previous, 0, 2, 0, 0, true, out);

        assertThat(out.x()).isCloseTo(0.1, within(1e-9));
        assertThat(out.y()).isCloseTo(0.2, within(1e-9));
        assertThat(out.z()).isCloseTo(0.05 - 0.4, within(1e-9));
    }

    @Test
    void eachKnockbackLevelAddsTheBoostAlongTheAttackersLook() {
        Motion still = new Motion();

        KnockbackMath.push(BEDWARS, still, 0, -1, 0, 2, true, out);

        assertThat(out.x()).isCloseTo(0, within(1e-9));
        assertThat(out.y()).isCloseTo(0.5, within(1e-9));
        assertThat(out.z()).isCloseTo(0.75 + 2 * 0.5, within(1e-9));
    }

    @Test
    void aRecoveringVictimOnlyTakesTheBoost() {
        Motion previous = new Motion().set(0.3, 0.1, 0);

        KnockbackMath.push(BEDWARS, previous, 0, -1, 0, 1, false, out);

        assertThat(out.x()).isCloseTo(0.3, within(1e-9));
        assertThat(out.y()).isCloseTo(0.2, within(1e-9));
        assertThat(out.z()).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void rotationUsesTheAttackersLookInsteadOfThePositions() {
        KnockbackProfile rotation = BEDWARS.with(KnockbackSetting.USE_ROTATION, "true");

        KnockbackMath.push(rotation, new Motion(), 5, 0, 180, 0, true, out);

        assertThat(out.x()).isCloseTo(0, within(1e-9));
        assertThat(out.z()).isCloseTo(-0.75, within(1e-9));
    }

    @Test
    void playersOnTheSameSpotArePushedAlongTheLook() {
        KnockbackMath.push(KnockbackProfile.VANILLA, new Motion(), 0, 0, 0, 0, true, out);

        assertThat(out.z()).isCloseTo(0.4, within(1e-9));
    }

    @Test
    void fallingFollowsGravityAndDragUntilTerminalSpeed() {
        assertThat(KnockbackMath.fall(0.4, 1)).isCloseTo((0.4 - 0.08) * 0.98, within(1e-12));
        assertThat(KnockbackMath.fall(0.4, 0)).isEqualTo(0.4);
        assertThat(KnockbackMath.fall(0, 1000)).isGreaterThanOrEqualTo(KnockbackMath.TERMINAL_Y);
    }

    @Test
    void slidingSlowsFasterOnTheGround() {
        assertThat(KnockbackMath.slide(0, true)).isEqualTo(1.0);
        assertThat(KnockbackMath.slide(1, true)).isCloseTo(0.546, within(1e-12));
        assertThat(KnockbackMath.slide(2, false)).isCloseTo(0.91 * 0.91, within(1e-12));
    }

    @Test
    void pingIsRoundedUpToTicks() {
        assertThat(KnockbackMath.pingTicks(0)).isZero();
        assertThat(KnockbackMath.pingTicks(1)).isEqualTo(1);
        assertThat(KnockbackMath.pingTicks(50)).isEqualTo(1);
        assertThat(KnockbackMath.pingTicks(120)).isEqualTo(3);
        assertThat(KnockbackMath.pingTicks(-5)).isZero();
    }
}
