package br.com.muxe.minigame.bedwars.game;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MagicMilk {

    public static final String ITEM_ID = "leite";
    public static final Duration TRAP_SHIELD = Duration.ofSeconds(30);
    public static final Set<String> NEGATIVE_EFFECTS = Set.of("BLINDNESS", "SLOW", "SLOW_DIGGING", "WEAKNESS", "POISON", "CONFUSION", "HUNGER", "WITHER", "HARM");

    private final Cooldowns shields = new Cooldowns(TRAP_SHIELD);

    public void drink(UUID player, Instant now) {
        shields.start(player, now);
    }

    public boolean shields(UUID player, Instant now) {
        return shields.remaining(player, now).isPresent();
    }

    public void forget(UUID player) {
        shields.forget(player);
    }

    public static List<String> negative(Collection<String> effects) {
        return effects.stream().filter(NEGATIVE_EFFECTS::contains).toList();
    }
}
