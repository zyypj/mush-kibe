package br.com.muxe.core.common.combat.knockback;

public final class KnockbackState {

    private static final int NONE = Integer.MIN_VALUE;

    private final Motion tracked = new Motion();
    private final Motion previous = new Motion();
    private final Motion pending = new Motion();
    private KnockbackProfile profile;
    private int trackedTick = NONE;
    private int pendingTick = NONE;
    private int lastKnockbackTick = NONE;
    private Object pendingAttacker;
    private boolean pushes;
    private boolean attackerSprinting;

    public KnockbackState(KnockbackProfile profile) {
        this.profile = profile;
    }

    public KnockbackProfile profile() {
        return profile;
    }

    public void profile(KnockbackProfile profile) {
        this.profile = profile;
    }

    public boolean prepare(KnockbackHit hit, int limiterTicks, Object attacker) {
        pendingTick = hit.tick();
        pendingAttacker = attacker;
        attackerSprinting = hit.attackerSprinting();
        int level = hit.knockbackLevel() + (hit.attackerSprinting() && !profile.ignoreSprint() ? 1 : 0);
        boolean limited = limiterTicks > 0 && lastKnockbackTick != NONE && hit.tick() - lastKnockbackTick < limiterTicks;
        if (limited || hit.victimRecovering() && level == 0) {
            pushes = false;
            return false;
        }
        previous(hit);
        KnockbackMath.push(profile, previous, hit.towardsAttackerX(), hit.towardsAttackerZ(), hit.attackerYaw(), level, !hit.victimRecovering(), pending);
        pushes = true;
        return true;
    }

    public boolean isPending(int tick) {
        return pendingTick != NONE && pendingTick == tick;
    }

    public boolean pushes() {
        return pushes;
    }

    public boolean attackerWasSprinting() {
        return attackerSprinting;
    }

    public Motion pending() {
        return pending;
    }

    public Object pendingAttacker() {
        return pendingAttacker;
    }

    public void applied(int tick) {
        tracked.set(pending);
        trackedTick = tick;
        lastKnockbackTick = tick;
        pendingTick = NONE;
        pendingAttacker = null;
    }

    public void discard() {
        pendingTick = NONE;
        pendingAttacker = null;
    }

    public void attacked(int tick, boolean onGround) {
        if (trackedTick == NONE) {
            return;
        }
        double slide = KnockbackMath.slide(tick - trackedTick, onGround);
        tracked.scaleHorizontal(slide * profile.attackerMultiplier());
        tracked.set(tracked.x(), KnockbackMath.fall(tracked.y(), tick - trackedTick), tracked.z());
        trackedTick = tick;
    }

    private void previous(KnockbackHit hit) {
        int elapsed = trackedTick == NONE ? 0 : hit.tick() - trackedTick;
        if (profile.restoreVelocity() || trackedTick == NONE) {
            previous.set(hit.serverMotion());
        } else {
            double slide = KnockbackMath.slide(elapsed, hit.victimOnGround());
            previous.set(tracked.x() * slide, tracked.y(), tracked.z() * slide);
        }
        if (profile.simulateY()) {
            double y;
            if (hit.victimOnGround()) {
                y = KnockbackMath.RESTING_Y;
            } else if (trackedTick != NONE) {
                y = KnockbackMath.fall(tracked.y(), elapsed + KnockbackMath.pingTicks(hit.ping()));
            } else {
                y = hit.serverMotion().y();
            }
            previous.set(previous.x(), y, previous.z());
        }
    }
}
