package br.com.muxe.core.bukkit.bossbar;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class BossBarPlacement {

    public static final double DISTANCE = 32;

    private static final double MOVE_DISTANCE_SQUARED = 1;
    private static final float TURN_DEGREES = 15;

    private BossBarPlacement() {
    }

    public static Location inFront(Location eyes, double distance) {
        Vector direction = eyes.getDirection().normalize().multiply(distance);
        return eyes.clone().add(direction);
    }

    public static boolean needsMove(Location carriedFor, Location now) {
        double dx = now.getX() - carriedFor.getX();
        double dy = now.getY() - carriedFor.getY();
        double dz = now.getZ() - carriedFor.getZ();
        if (dx * dx + dy * dy + dz * dz >= MOVE_DISTANCE_SQUARED) {
            return true;
        }
        return angle(carriedFor.getYaw(), now.getYaw()) >= TURN_DEGREES || Math.abs(carriedFor.getPitch() - now.getPitch()) >= TURN_DEGREES;
    }

    private static float angle(float from, float to) {
        float difference = Math.abs(from - to) % 360;
        return difference > 180 ? 360 - difference : difference;
    }
}
