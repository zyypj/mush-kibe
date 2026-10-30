package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.minigame.bedwars.game.GeneratorTimer;
import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings.Forges;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

final class RoomForges {

    static final String FORGE_ITEM = "muxe-forge";

    private static final double GROUND_RADIUS = 3;
    private static final double DROP_HEIGHT = 1.3;
    private static final double HOLOGRAM_HEIGHT = 3.4;

    private final Plugin plugin;
    private final BedWarsSettings settings;
    private final TextHolograms holograms = TextHolograms.standard();
    private final List<Forge> forges = new ArrayList<>();
    private final Map<UUID, Map<Forge, SpawnedHologram>> shown = new HashMap<>();
    private int diamondTier = 1;
    private int emeraldTier = 1;

    RoomForges(Plugin plugin, BedWarsSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    void island(TeamColor team, Location location, Material material, Rate rate) {
        forges.add(new Forge(Kind.ISLAND, team, drop(location), material, new GeneratorTimer(rate), rate));
    }

    void shared(Location location, Material material) {
        Forges config = settings.forges();
        Rate rate = (material == Material.DIAMOND ? config.diamond() : config.emerald()).getFirst().rate();
        forges.add(new Forge(Kind.SHARED, null, drop(location), material, new GeneratorTimer(rate), rate));
    }

    void tier(Material material, int tier) {
        Forges config = settings.forges();
        List<BedWarsSettings.Tier> tiers = material == Material.DIAMOND ? config.diamond() : config.emerald();
        Rate rate = tiers.get(Math.min(tier, tiers.size()) - 1).rate();
        if (material == Material.DIAMOND) {
            diamondTier = tier;
        } else {
            emeraldTier = tier;
        }
        for (Forge forge : forges) {
            if (forge.kind() == Kind.SHARED && forge.material() == material) {
                forge.timer().apply(rate);
            }
        }
    }

    void forgeLevel(TeamColor team, int level, Location emeraldAt) {
        double speed = switch (level) {
            case 1 -> 1.5;
            case 2 -> 2;
            case 3 -> 2;
            case 4 -> 3;
            default -> 1;
        };
        for (Forge forge : forges) {
            if (forge.kind() != Kind.ISLAND || !team.equals(forge.team()) || forge.material() == Material.EMERALD) {
                continue;
            }
            Rate base = forge.base();
            forge.timer().apply(new Rate(base.delaySeconds() / speed, base.amount(), (int) Math.round(base.spawnLimit() * speed)));
        }
        boolean hasEmerald = forges.stream().anyMatch(forge -> forge.kind() == Kind.ISLAND && team.equals(forge.team()) && forge.material() == Material.EMERALD);
        if (level >= 3 && !hasEmerald && emeraldAt != null) {
            island(team, emeraldAt, Material.EMERALD, settings.forges().islandEmerald());
        }
        if (level >= 4) {
            Rate emerald = settings.forges().islandEmerald();
            for (Forge forge : forges) {
                if (forge.kind() == Kind.ISLAND && team.equals(forge.team()) && forge.material() == Material.EMERALD) {
                    forge.timer().apply(new Rate(emerald.delaySeconds(), emerald.amount() * 2, emerald.spawnLimit() * 2));
                }
            }
        }
    }

    void cycle() {
        for (Forge forge : forges) {
            Location at = forge.location();
            if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
                continue;
            }
            int amount = forge.timer().cycle(grounded(forge));
            if (amount > 0) {
                Item item = at.getWorld().dropItem(at, new ItemStack(forge.material(), amount));
                item.setVelocity(new Vector());
                item.setPickupDelay(0);
                if (forge.kind() == Kind.ISLAND && (forge.material() == Material.IRON_INGOT || forge.material() == Material.GOLD_INGOT)) {
                    item.setMetadata(FORGE_ITEM, new FixedMetadataValue(plugin, Boolean.TRUE));
                }
            }
        }
    }

    void holograms(List<Player> viewers, boolean resend) {
        for (Player viewer : viewers) {
            Map<Forge, SpawnedHologram> mine = shown.computeIfAbsent(viewer.getUniqueId(), ignored -> new HashMap<>());
            for (Forge forge : forges) {
                if (forge.kind() != Kind.SHARED) {
                    continue;
                }
                List<String> lines = lines(forge);
                SpawnedHologram current = mine.get(forge);
                if (current == null || !lines.equals(forge.lastLines(viewer.getUniqueId()))) {
                    if (current != null) {
                        current.remove();
                    }
                    Location top = forge.location().clone().add(0, HOLOGRAM_HEIGHT - DROP_HEIGHT, 0);
                    mine.put(forge, holograms.show(viewer, top, lines));
                    forge.lastLines(viewer.getUniqueId(), lines);
                } else if (resend) {
                    current.resend();
                }
            }
        }
    }

    void hide(UUID viewer) {
        Map<Forge, SpawnedHologram> mine = shown.remove(viewer);
        if (mine != null) {
            mine.values().forEach(SpawnedHologram::remove);
        }
        forges.forEach(forge -> forge.forget(viewer));
    }

    void clear() {
        List.copyOf(shown.keySet()).forEach(this::hide);
        forges.clear();
    }

    int diamondTier() {
        return diamondTier;
    }

    int emeraldTier() {
        return emeraldTier;
    }

    private List<String> lines(Forge forge) {
        List<String> template = forge.material() == Material.DIAMOND ? settings.holograms().diamond() : settings.holograms().emerald();
        int tier = forge.material() == Material.DIAMOND ? diamondTier : emeraldTier;
        return template.stream()
            .map(line -> line.replace("{tier}", BedWarsSettings.roman(tier)).replace("{seconds}", String.valueOf(forge.timer().secondsLeft())))
            .toList();
    }

    private static int grounded(Forge forge) {
        int total = 0;
        for (Entity entity : forge.location().getWorld().getNearbyEntities(forge.location(), GROUND_RADIUS, GROUND_RADIUS, GROUND_RADIUS)) {
            if (entity instanceof Item item && item.getItemStack().getType() == forge.material()) {
                total += item.getItemStack().getAmount();
            }
        }
        return total;
    }

    private static Location drop(Location block) {
        return block.clone().add(0, DROP_HEIGHT, 0);
    }

    enum Kind {
        ISLAND,
        SHARED
    }

    static final class Forge {

        private final Kind kind;
        private final TeamColor team;
        private final Location location;
        private final Material material;
        private final GeneratorTimer timer;
        private final Rate base;
        private final Map<UUID, List<String>> lastLines = new HashMap<>();

        Forge(Kind kind, TeamColor team, Location location, Material material, GeneratorTimer timer, Rate base) {
            this.kind = kind;
            this.team = team;
            this.location = location;
            this.material = material;
            this.timer = timer;
            this.base = base;
        }

        Kind kind() {
            return kind;
        }

        TeamColor team() {
            return team;
        }

        Location location() {
            return location;
        }

        Material material() {
            return material;
        }

        GeneratorTimer timer() {
            return timer;
        }

        Rate base() {
            return base;
        }

        List<String> lastLines(UUID viewer) {
            return lastLines.get(viewer);
        }

        void lastLines(UUID viewer, List<String> lines) {
            lastLines.put(viewer, lines);
        }

        void forget(UUID viewer) {
            lastLines.remove(viewer);
        }
    }
}
