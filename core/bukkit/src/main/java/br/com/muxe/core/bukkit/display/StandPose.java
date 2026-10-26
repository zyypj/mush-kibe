package br.com.muxe.core.bukkit.display;

import org.bukkit.util.EulerAngle;

public record StandPose(double x, double y, double z) {

    public static final StandPose ZERO = new StandPose(0, 0, 0);

    public EulerAngle toEulerAngle() {
        return new EulerAngle(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
    }
}
