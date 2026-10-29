package br.com.muxe.core.bukkit.bossbar;

import br.com.muxe.core.bukkit.text.Colors;
import java.util.List;

public record BossBarFrames(List<String> frames, long ticksPerFrame) {

    public BossBarFrames {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("A boss bar needs at least one frame");
        }
        if (ticksPerFrame < 1) {
            throw new IllegalArgumentException("Frames last at least one tick");
        }
        frames = frames.stream().map(Colors::translate).toList();
    }

    public String at(long tick) {
        return frames.get((int) ((tick / ticksPerFrame) % frames.size()));
    }
}
