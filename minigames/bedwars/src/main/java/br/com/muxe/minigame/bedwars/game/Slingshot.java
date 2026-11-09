package br.com.muxe.minigame.bedwars.game;

import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class Slingshot {

    public static final String ITEM_ID = "estilingue";
    public static final String ITEM_NAME = "&cEstilingue";
    public static final Duration COOLDOWN = Duration.ofSeconds(7);
    public static final List<Float> SOUND_PITCHES = List.of(1.25f, 1.5f, 1.75f, 2.0f);
    public static final String SOUND = "mob.slime.small";
    public static final int SOUND_GAP_TICKS = 4;
    public static final int SLIME_BLOCK_ID = 165;
    static final double FORWARD = 1.5625;
    static final double LIFT = 1.0;
    private static final String LABEL = "ESTILINGUE:";

    private final Map<UUID, Instant> readyAt = new HashMap<>();

    public static Vec push(Vec look) {
        return new Vec(look.x() * FORWARD, LIFT, look.z() * FORWARD);
    }

    public boolean use(UUID player, Instant now) {
        return use(player, now, COOLDOWN);
    }

    public boolean use(UUID player, Instant now, Duration cooldown) {
        if (remaining(player, now).isPresent()) {
            return false;
        }
        readyAt.put(player, now.plus(cooldown));
        return true;
    }

    public Optional<Duration> remaining(UUID player, Instant now) {
        Instant ready = readyAt.get(player);
        if (ready == null || !ready.isAfter(now)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, ready));
    }

    public void forget(UUID player) {
        readyAt.remove(player);
    }

    public static String actionBar(Duration remaining) {
        return actionBar(remaining, COOLDOWN);
    }

    public static String actionBar(Duration remaining, Duration cooldown) {
        long total = Math.max(1, cooldown.toMillis() / 100);
        long tenths = (remaining.toMillis() + 99) / 100;
        int underlined = (int) Math.min(LABEL.length(), (LABEL.length() * tenths + total - 1) / total);
        StringBuilder text = new StringBuilder();
        if (underlined > 0) {
            text.append("§c§n§l").append(LABEL, 0, underlined);
        }
        if (underlined < LABEL.length()) {
            text.append("§c§l").append(LABEL.substring(underlined));
        }
        return text.append("§r ").append(tenths / 10).append(',').append(tenths % 10).append('s').toString();
    }
}
