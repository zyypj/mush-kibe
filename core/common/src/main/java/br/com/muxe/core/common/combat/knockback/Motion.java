package br.com.muxe.core.common.combat.knockback;

public final class Motion {

    private double x;
    private double y;
    private double z;

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public Motion set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Motion set(Motion other) {
        return set(other.x, other.y, other.z);
    }

    public Motion scaleHorizontal(double factor) {
        x *= factor;
        z *= factor;
        return this;
    }

    @Override
    public String toString() {
        return "Motion[" + x + ", " + y + ", " + z + "]";
    }
}
