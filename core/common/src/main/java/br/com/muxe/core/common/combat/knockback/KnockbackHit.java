package br.com.muxe.core.common.combat.knockback;

public final class KnockbackHit {

    private int tick;
    private int ping;
    private boolean victimOnGround;
    private boolean victimRecovering;
    private final Motion serverMotion = new Motion();
    private double towardsAttackerX;
    private double towardsAttackerZ;
    private float attackerYaw;
    private int knockbackLevel;
    private boolean attackerSprinting;

    public KnockbackHit at(int tick, int ping) {
        this.tick = tick;
        this.ping = Math.max(0, ping);
        return this;
    }

    public KnockbackHit victim(boolean onGround, boolean recovering, double motionX, double motionY, double motionZ) {
        this.victimOnGround = onGround;
        this.victimRecovering = recovering;
        serverMotion.set(motionX, motionY, motionZ);
        return this;
    }

    public KnockbackHit attacker(double towardsAttackerX, double towardsAttackerZ, float yaw, int knockbackLevel, boolean sprinting) {
        this.towardsAttackerX = towardsAttackerX;
        this.towardsAttackerZ = towardsAttackerZ;
        this.attackerYaw = yaw;
        this.knockbackLevel = Math.max(0, knockbackLevel);
        this.attackerSprinting = sprinting;
        return this;
    }

    public int tick() {
        return tick;
    }

    public int ping() {
        return ping;
    }

    public boolean victimOnGround() {
        return victimOnGround;
    }

    public boolean victimRecovering() {
        return victimRecovering;
    }

    public Motion serverMotion() {
        return serverMotion;
    }

    public double towardsAttackerX() {
        return towardsAttackerX;
    }

    public double towardsAttackerZ() {
        return towardsAttackerZ;
    }

    public float attackerYaw() {
        return attackerYaw;
    }

    public int knockbackLevel() {
        return knockbackLevel;
    }

    public boolean attackerSprinting() {
        return attackerSprinting;
    }
}
