package br.com.muxe.core.bukkit.cosmetic.animation;

public record Vec(double x, double y, double z) {

    public static final Vec ZERO = new Vec(0, 0, 0);

    public Vec add(Vec other) {
        return new Vec(x + other.x, y + other.y, z + other.z);
    }

    public Vec add(double dx, double dy, double dz) {
        return new Vec(x + dx, y + dy, z + dz);
    }

    public Vec subtract(Vec other) {
        return new Vec(x - other.x, y - other.y, z - other.z);
    }

    public Vec multiply(double factor) {
        return new Vec(x * factor, y * factor, z * factor);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec normalize() {
        double length = length();
        return length == 0 ? ZERO : multiply(1 / length);
    }

    public double distance(Vec other) {
        return subtract(other).length();
    }

    public float yawTowards(Vec target) {
        double dx = target.x - x;
        double dz = target.z - z;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
