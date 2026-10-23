package br.com.muxe.core.bukkit.effect;

public record ParticleColor(int red, int green, int blue) {

    public static final ParticleColor GREEN = new ParticleColor(40, 200, 60);
    public static final ParticleColor WHITE = new ParticleColor(255, 255, 255);
    public static final ParticleColor AQUA = new ParticleColor(85, 255, 255);
    public static final ParticleColor YELLOW = new ParticleColor(255, 255, 85);

    private static final float MIN_CHANNEL = 0.001f;

    public ParticleColor {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
            throw new IllegalArgumentException("Color channels go from 0 to 255");
        }
    }

    float redChannel() {
        return Math.max(MIN_CHANNEL, red / 255f);
    }

    float greenChannel() {
        return green / 255f;
    }

    float blueChannel() {
        return blue / 255f;
    }
}
