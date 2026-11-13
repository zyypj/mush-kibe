package br.com.muxe.minigame.bedwars.game;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Rewind {

    public static final String ITEM_ID = "rebobinar";
    public static final Duration COOLDOWN = Duration.ofSeconds(7);
    public static final int HISTORY_TICKS = 60;
    public static final int STRIDE = 2;
    public static final int HOLD_TICKS = 5;
    public static final double JUMP = 8.0;
    public static final double CAMERA_DROP = 0.93;
    public static final float SOUND_PITCH = 2.0f;

    private final Deque<Sample> samples = new ArrayDeque<>();

    public void record(Sample sample) {
        Sample last = samples.peekLast();
        if (last != null && last.distanceSquared(sample) > JUMP * JUMP) {
            samples.clear();
        }
        samples.addLast(sample);
        while (samples.size() > HISTORY_TICKS + 1) {
            samples.removeFirst();
        }
    }

    public List<Sample> trail() {
        List<Sample> ordered = new ArrayList<>(samples);
        List<Sample> trail = new ArrayList<>();
        for (int index = ordered.size() - 1 - STRIDE; index >= 0; index -= STRIDE) {
            trail.add(ordered.get(index));
        }
        return trail;
    }

    public void clear() {
        samples.clear();
    }

    public record Sample(double x, double y, double z, float yaw, float pitch) {

        double distanceSquared(Sample other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
