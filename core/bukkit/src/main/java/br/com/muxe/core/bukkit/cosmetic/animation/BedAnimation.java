package br.com.muxe.core.bukkit.cosmetic.animation;

import java.util.random.RandomGenerator;

public interface BedAnimation {

    String id();

    void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random);
}
