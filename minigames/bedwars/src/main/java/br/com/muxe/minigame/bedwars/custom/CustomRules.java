package br.com.muxe.minigame.bedwars.custom;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.RoomEditor.LimitType;
import br.com.muxe.minigame.api.timeline.Timeline;
import br.com.muxe.minigame.api.timeline.Timeline.Entry;
import br.com.muxe.minigame.bedwars.game.BedWarsEvent;
import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CustomRules(RoomOptions options, boolean custom) {

    public static final CustomRules NONE = new CustomRules(RoomOptions.EMPTY, false);
    public static final String FIREBALL_FIGHT = "fireball-fight";
    public static final String DRAGON_EGG = "dragon-egg";
    public static final double FIGHT_LIFT = 1.5;
    public static final double FIGHT_DAMAGE = 0.5;
    public static final List<String> EVENT_KEYS = List.of("diamond-2", "emerald-2", "diamond-3", "emerald-3", "beds-destroyed", "dragons", "game-end");

    public static CustomRules of(Optional<RoomOptions> options) {
        return options.map(found -> new CustomRules(found, true)).orElse(NONE);
    }

    public Rate island(Rate base) {
        return tune(base, options.decimal("generators.forge.speed", 1), Optional.empty());
    }

    public boolean diamonds() {
        return options.flag("generators.diamond.enabled", true);
    }

    public boolean emeralds() {
        return options.flag("generators.emerald.enabled", true);
    }

    public Rate diamond(Rate base) {
        return tune(base, options.decimal("generators.diamond.speed", 1), amount("generators.diamond.amount"));
    }

    public Rate emerald(Rate base) {
        return tune(base, options.decimal("generators.emerald.speed", 1), amount("generators.emerald.amount"));
    }

    public static Rate tune(Rate base, double speed, Optional<Integer> amount) {
        double factor = speed > 0 ? speed : 1;
        int drops = amount.orElse(base.amount());
        int limit = base.spawnLimit() <= 0 ? base.spawnLimit() : Math.max(base.spawnLimit(), base.spawnLimit() * drops / Math.max(1, base.amount()));
        return new Rate(base.delaySeconds() / factor, Math.max(1, drops), limit);
    }

    public boolean sells(String key) {
        return sells(key, true);
    }

    public boolean sells(String key, boolean base) {
        return options.flag("shop." + key + ".enabled", base);
    }

    public int price(String key, int base) {
        if (options.flag("shop.free", false)) {
            return 0;
        }
        return Math.max(0, options.integer("shop." + key + ".price", base));
    }

    public String currency(String key, String base) {
        return options.get("shop." + key + ".currency", base);
    }

    public LimitType limitType(String key) {
        return LimitType.parse(options.get("shop." + key + ".limit-type", LimitType.NONE.key()));
    }

    public int limit(String key) {
        return Math.max(1, options.integer("shop." + key + ".limit", 1));
    }

    public boolean upgrades(String id, boolean base) {
        return options.flag("upgrades." + id + ".enabled", base);
    }

    public int upgradePrice(String id, int level, int base) {
        if (options.flag("upgrades.free", false)) {
            return 0;
        }
        return Math.max(0, options.integer("upgrades." + id + "." + level + ".price", base));
    }

    public String upgradeCurrency(String id, int level, String base) {
        return options.get("upgrades." + id + "." + level + ".currency", base);
    }

    public boolean fireballFight() {
        return FIREBALL_FIGHT.equals(options.get("items.fireball", "default"));
    }

    public Duration slingshotCooldown(Duration base) {
        return Duration.ofSeconds(Math.max(0, options.integer("items.slingshot-cooldown", (int) base.toSeconds())));
    }

    public boolean dragonEgg() {
        return DRAGON_EGG.equals(options.get("others.totem", "bed"));
    }

    public boolean creative() {
        return options.flag("others.creative", false);
    }

    public Timeline<BedWarsEvent> timeline(Timeline<BedWarsEvent> base) {
        if (!custom) {
            return base;
        }
        List<Entry<BedWarsEvent>> ordered = base.entries();
        List<Entry<BedWarsEvent>> changed = new ArrayList<>();
        int previousBase = 0;
        int previous = 0;
        for (int index = 0; index < ordered.size(); index++) {
            Entry<BedWarsEvent> entry = ordered.get(index);
            String key = index < EVENT_KEYS.size() ? EVENT_KEYS.get(index) : "event-" + index;
            int interval = Math.max(0, options.integer("events." + key + ".seconds", entry.at() - previousBase));
            previousBase = entry.at();
            previous += interval;
            if (options.flag("events." + key + ".enabled", true) || entry.event().kind() == BedWarsEvent.Kind.GAME_END) {
                changed.add(new Entry<>(entry.event(), previous, entry.label()));
            }
        }
        return new Timeline<>(changed);
    }

    public static List<Integer> intervals(Timeline<BedWarsEvent> base) {
        List<Integer> intervals = new ArrayList<>();
        int previous = 0;
        for (Entry<BedWarsEvent> entry : base.entries()) {
            intervals.add(entry.at() - previous);
            previous = entry.at();
        }
        return intervals;
    }

    private Optional<Integer> amount(String key) {
        return options.has(key) ? Optional.of(Math.max(1, options.integer(key, 1))) : Optional.empty();
    }
}
