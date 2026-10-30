package br.com.muxe.minigame.bedwars.cosmetic.animation;

import br.com.muxe.core.bukkit.cosmetic.animation.BedAnimation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BedAnimations {

    private static final List<BedAnimation> ALL = List.of(
        new SantaHelperAnimation(),
        new AvalancheAnimation(),
        new RabbitsAnimation(),
        new BirdsAnimation(),
        new FrostAnimation(),
        new FireworksAnimation(),
        new PumpkinHarvestAnimation(),
        new SpookyAnimation(),
        new DesertAnimation(),
        new PaintAnimation(),
        new CannonballAnimation(),
        new EndermanAnimation(),
        new SaltAnimation()
    );

    private final Map<String, BedAnimation> byId = new LinkedHashMap<>();

    public BedAnimations() {
        for (BedAnimation animation : ALL) {
            byId.put(animation.id(), animation);
        }
    }

    public Optional<BedAnimation> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<BedAnimation> all() {
        return List.copyOf(byId.values());
    }
}
