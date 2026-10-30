package br.com.muxe.minigame.engine.setup;

import java.util.Locale;

public record Point(double x, double y, double z, float yaw, float pitch) {

    public static Point block(int x, int y, int z) {
        return new Point(x + 0.5, y, z + 0.5, 0, 0);
    }

    public Point centered() {
        return new Point(Math.floor(x) + 0.5, Math.floor(y), Math.floor(z) + 0.5, snap(yaw), 0);
    }

    public int blockX() {
        return (int) Math.floor(x);
    }

    public int blockY() {
        return (int) Math.floor(y);
    }

    public int blockZ() {
        return (int) Math.floor(z);
    }

    public double distanceSquared(Point other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Point shifted(double dx, double dz) {
        return new Point(x + dx, y, z + dz, yaw, pitch);
    }

    public String describe() {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", x, y, z);
    }

    static float snap(float yaw) {
        float normalized = ((yaw % 360) + 360) % 360;
        return (Math.round(normalized / 45f) * 45) % 360;
    }
}
