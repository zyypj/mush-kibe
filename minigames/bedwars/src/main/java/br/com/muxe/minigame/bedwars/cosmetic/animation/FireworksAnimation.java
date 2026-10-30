package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import br.com.muxe.core.bukkit.cosmetic.animation.BedFrame;
import br.com.muxe.core.bukkit.cosmetic.animation.Stage;
import br.com.muxe.core.bukkit.cosmetic.animation.Timeline;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.util.random.RandomGenerator;

public final class FireworksAnimation implements BedAnimation {

    static final int COLOR = 0x993333;
    static final int SHADE = 0x4C1919;
    static final int END = 3;

    @Override
    public String id() {
        return "fogos-de-artificio";
    }

    @Override
    public void script(BedFrame bed, Vec breaker, Stage stage, Timeline timeline, RandomGenerator random) {
        Kit.flash(stage, timeline, 0, bed.middle().add(0, 1, 0), Kit.burst(Kit.LARGE_BALL, false, true, COLOR, SHADE));
        timeline.lasts(END);
    }
}
