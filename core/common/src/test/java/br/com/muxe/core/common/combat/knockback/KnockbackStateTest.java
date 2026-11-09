package br.com.muxe.core.common.combat.knockback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class KnockbackStateTest {

    private static final KnockbackProfile BEDWARS = new KnockbackProfile(0.6, 0.75, 0.1, 0.5, 0.4, 2.0, 2.0, 0.6,
        false, true, false, true, false, ClickLimiter.NORMAL);

    private final KnockbackHit hit = new KnockbackHit();
    private final Object attacker = new Object();

    @Test
    void aHitIsPendingOnlyOnItsOwnTick() {
        KnockbackState state = new KnockbackState(BEDWARS);

        boolean prepared = state.prepare(groundHit(100), 0, attacker);

        assertThat(prepared).isTrue();
        assertThat(state.isPending(100)).isTrue();
        assertThat(state.isPending(101)).isFalse();
        assertThat(state.pendingAttacker()).isSameAs(attacker);
    }

    @Test
    void ignoringSprintDropsTheSprintBoost() {
        KnockbackState state = new KnockbackState(BEDWARS);

        state.prepare(groundHit(1).attacker(0, -1, 0, 0, true), 0, attacker);

        assertThat(state.pending().z()).isCloseTo(0.75, within(1e-9));
        assertThat(state.pending().y()).isCloseTo(0.4, within(1e-9));
    }

    @Test
    void sprintAddsALevelWhenNotIgnored() {
        KnockbackState state = new KnockbackState(KnockbackProfile.VANILLA);

        state.prepare(groundHit(1).attacker(0, -1, 0, 0, true), 0, attacker);

        assertThat(state.pending().z()).isCloseTo(0.4 + 0.5, within(1e-9));
    }

    @Test
    void simulatingYTreatsAPlayerOnTheGroundAsResting() {
        KnockbackState state = new KnockbackState(BEDWARS.with(KnockbackSetting.VERTICAL, "0.36").with(KnockbackSetting.VERTICAL_LIMIT, "1"));

        state.prepare(groundHit(1).victim(true, false, 0, 0.9, 0), 0, attacker);

        assertThat(state.pending().y()).isCloseTo(KnockbackMath.RESTING_Y / 2 + 0.36, within(1e-9));
    }

    @Test
    void simulatingYFollowsTheLastKnockbackThroughTheAirAndThePing() {
        KnockbackProfile profile = BEDWARS.with(KnockbackSetting.VERTICAL_LIMIT, "5");
        KnockbackState state = new KnockbackState(profile);
        state.prepare(groundHit(10), 0, attacker);
        state.applied(10);
        double launched = state.pending().y();

        state.prepare(airHit(14, 100), 0, attacker);

        double expected = KnockbackMath.fall(launched, 4 + 2) / 2.0 + 0.6;
        assertThat(state.pending().y()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void keepingTheImpulseUsesTheTrackedMotionSlowedBySliding() {
        KnockbackProfile noSimulation = BEDWARS.with(KnockbackSetting.SIMULATE_Y, "false");
        KnockbackState state = new KnockbackState(noSimulation);
        state.prepare(groundHit(10), 0, attacker);
        state.applied(10);
        double firstZ = state.pending().z();

        state.prepare(airHit(12, 0).victim(false, false, 9, 9, 9), 0, attacker);

        double expected = firstZ * 0.91 * 0.91 / 2.0 + 0.75;
        assertThat(state.pending().z()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void restoringTheImpulseUsesTheServerMotion() {
        KnockbackProfile restore = BEDWARS.with(KnockbackSetting.RESTORE_VELOCITY, "true").with(KnockbackSetting.SIMULATE_Y, "false");
        KnockbackState state = new KnockbackState(restore);
        state.prepare(groundHit(10), 0, attacker);
        state.applied(10);

        state.prepare(airHit(12, 0).victim(false, false, 0, 0, 0.2), 0, attacker);

        assertThat(state.pending().z()).isCloseTo(0.1 + 0.75, within(1e-9));
    }

    @Test
    void theClickLimiterSkipsKnockbackTooSoonAfterTheLastOne() {
        KnockbackState state = new KnockbackState(BEDWARS);
        state.prepare(groundHit(10), 0, attacker);
        state.applied(10);

        boolean early = state.prepare(groundHit(12), 4, attacker);
        boolean late = state.prepare(groundHit(14), 4, attacker);

        assertThat(early).isFalse();
        assertThat(late).isTrue();
    }

    @Test
    void aLimitedHitStaysPendingSoTheServerImpulseIsSuppressed() {
        KnockbackState state = new KnockbackState(BEDWARS);
        state.prepare(groundHit(10), 0, attacker);
        state.applied(10);

        state.prepare(groundHit(11), 4, attacker);

        assertThat(state.isPending(11)).isTrue();
        assertThat(state.pushes()).isFalse();
    }

    @Test
    void aRecoveringVictimWithoutBoostIsNotPushed() {
        KnockbackState state = new KnockbackState(BEDWARS);

        boolean pushed = state.prepare(hit.at(5, 0).victim(true, true, 0, 0, 0).attacker(0, -1, 0, 0, true), 0, attacker);

        assertThat(pushed).isFalse();
        assertThat(state.pushes()).isFalse();
    }

    @Test
    void applyingClearsThePendingHit() {
        KnockbackState state = new KnockbackState(BEDWARS);
        state.prepare(groundHit(3), 0, attacker);

        state.applied(3);

        assertThat(state.isPending(3)).isFalse();
        assertThat(state.pendingAttacker()).isNull();
    }

    @Test
    void attackingSlowsTheAttackersOwnTrackedMotion() {
        KnockbackProfile noSimulation = BEDWARS.with(KnockbackSetting.SIMULATE_Y, "false");
        KnockbackState state = new KnockbackState(noSimulation);
        state.prepare(groundHit(10), 0, attacker);
        state.applied(10);
        double firstZ = state.pending().z();

        state.attacked(10, false);
        state.prepare(airHit(10, 0), 0, attacker);

        assertThat(state.pending().z()).isCloseTo(firstZ * 0.6 / 2.0 + 0.75, within(1e-9));
    }

    private KnockbackHit groundHit(int tick) {
        return hit.at(tick, 0).victim(true, false, 0, KnockbackMath.RESTING_Y, 0).attacker(0, -1, 0, 0, false);
    }

    private KnockbackHit airHit(int tick, int ping) {
        return hit.at(tick, ping).victim(false, false, 0, 0, 0).attacker(0, -1, 0, 0, false);
    }
}
