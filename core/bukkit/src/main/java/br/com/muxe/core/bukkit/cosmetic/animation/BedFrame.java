package br.com.muxe.core.bukkit.cosmetic.animation;

public record BedFrame(Vec foot, Vec forward) {

    public BedFrame {
        forward = new Vec(forward.x(), 0, forward.z()).normalize();
        if (forward.length() == 0) {
            throw new IllegalArgumentException("A bed needs a horizontal facing");
        }
    }

    public static BedFrame of(int footX, int footY, int footZ, int headX, int headZ) {
        return new BedFrame(new Vec(footX + 0.5, footY, footZ + 0.5), new Vec(headX - footX, 0, headZ - footZ));
    }

    public Vec right() {
        return new Vec(-forward.z(), 0, forward.x());
    }

    public Vec head() {
        return foot.add(forward);
    }

    public Vec middle() {
        return foot.add(forward.multiply(0.5));
    }

    public Vec at(double forwardBlocks, double up, double rightBlocks) {
        return foot.add(forward.multiply(forwardBlocks)).add(0, up, 0).add(right().multiply(rightBlocks));
    }

    public Vec around(Vec center, double radius, double degrees, double up) {
        double radians = Math.toRadians(degrees);
        return center.add(Math.cos(radians) * radius, up, Math.sin(radians) * radius);
    }

    public float yaw() {
        return (float) Math.toDegrees(Math.atan2(-forward.x(), forward.z()));
    }
}
