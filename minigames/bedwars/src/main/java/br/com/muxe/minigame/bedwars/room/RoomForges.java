package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.core.bukkit.display.Display;
import br.com.muxe.core.bukkit.display.DisplayService;
import br.com.muxe.core.bukkit.display.DisplayStand;
import br.com.muxe.core.bukkit.display.StandPose;
import br.com.muxe.core.bukkit.hologram.SpawnedHologram;
import br.com.muxe.core.bukkit.hologram.TextHolograms;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.minigame.bedwars.custom.CustomRules;
import br.com.muxe.minigame.bedwars.game.ForgeHeights;
import br.com.muxe.minigame.bedwars.game.ForgeLayout;
import br.com.muxe.minigame.bedwars.game.ForgeLayout.Cell;
import br.com.muxe.minigame.bedwars.game.GeneratorTimer;
import br.com.muxe.minigame.bedwars.game.GeneratorTimer.Rate;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings.Forges;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

final class RoomForges {

    static final String FORGE_ITEM = ForgePickup.FORGE_ITEM;

    static final String CELL_MARKER = "muxe-forge-";

    private static final double GROUND_RADIUS = 3;

    private final Plugin plugin;
    private final BedWarsSettings settings;
    private final DisplayService displays;
    private final TextHolograms holograms = TextHolograms.standard(ForgeHeights.HOLOGRAM_SPACING);
    private final List<Forge> forges = new ArrayList<>();
    private final Map<UUID, Map<Forge, SpawnedHologram>> shown = new HashMap<>();
    private CustomRules rules = CustomRules.NONE;
    private int diamondTier = 1;
    private int emeraldTier = 1;

    RoomForges(Plugin plugin, BedWarsSettings settings, DisplayService displays) {
        this.plugin = plugin;
        this.settings = settings;
        this.displays = displays;
    }

    void island(TeamColor team, Location location, Material material, Rate rate) {
        island(team, location, material, rate, ForgeLayout.single());
    }

    void rules(CustomRules value) {
        rules = value;
    }

    void island(TeamColor team, Location location, Material material, Rate rate, ForgeLayout layout) {
        Rate tuned = rules.island(rate);
        forges.add(new Forge(Kind.ISLAND, team, drop(location), material, new GeneratorTimer(tuned), tuned, layout, null));
    }

    private Rate shared(Material material, Rate rate) {
        return material == Material.DIAMOND ? rules.diamond(rate) : rules.emerald(rate);
    }

    void shared(Location location, Material material) {
        Forges config = settings.forges();
        if (material == Material.DIAMOND ? !rules.diamonds() : !rules.emeralds()) {
            return;
        }
        Rate rate = shared(material, (material == Material.DIAMOND ? config.diamond() : config.emerald()).getFirst().rate());
        String displayId = "forge-" + UUID.randomUUID();
        Location drop = drop(location);
        Material block = material == Material.DIAMOND ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
        displays.put(new Display(displayId, drop, List.of(new DisplayStand(0, ForgeHeights.BLOCK_ABOVE_DROP, 0, 0f, false, false,
            Optional.of(ItemBuilder.of(block)), Optional.empty(), StandPose.ZERO, StandPose.ZERO, ForgeHeights.BLOCK_MOTION)), List.of()));
        forges.add(new Forge(Kind.SHARED, null, drop, material, new GeneratorTimer(rate), rate, ForgeLayout.single(), displayId));
    }

    void tier(Material material, int tier) {
        Forges config = settings.forges();
        List<BedWarsSettings.Tier> tiers = material == Material.DIAMOND ? config.diamond() : config.emerald();
        Rate rate = shared(material, tiers.get(Math.min(tier, tiers.size()) - 1).rate());
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
            Rate emerald = rules.island(settings.forges().islandEmerald());
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
            if (amount <= 0) {
                continue;
            }
            if (forge.spreads()) {
                for (int index = 0; index < amount; index++) {
                    int drop = forge.nextDrop();
                    Cell cell = forge.layout().cellAt(drop);
                    spawn(forge, at.clone().add(cell.dx(), 0, cell.dz()), 1, forge.layout().cellIndexAt(drop));
                }
            } else {
                spawn(forge, at, amount, 0);
            }
        }
    }

    private void spawn(Forge forge, Location at, int amount, int cell) {
        ItemStack stack = new ItemStack(forge.material(), amount);
        if (!settings.forges().stackItems()) {
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(CELL_MARKER + cell);
            stack.setItemMeta(meta);
        }
        Item item = at.getWorld().dropItem(at, stack);
        item.setVelocity(new Vector());
        item.setPickupDelay(0);
        item.setMetadata(FORGE_ITEM, new FixedMetadataValue(plugin, Boolean.TRUE));
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
                    Location top = forge.location().clone().add(0, ForgeHeights.HOLOGRAM_TOP_ABOVE_DROP, 0);
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
        forges.stream().map(Forge::displayId).filter(Objects::nonNull).forEach(displays::remove);
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
        return block.clone().add(0, ForgeHeights.DROP_ABOVE_POINT, 0);
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
        private final ForgeLayout layout;
        private final String displayId;
        private final Map<UUID, List<String>> lastLines = new HashMap<>();
        private int drops;

        Forge(Kind kind, TeamColor team, Location location, Material material, GeneratorTimer timer, Rate base, ForgeLayout layout, String displayId) {
            this.kind = kind;
            this.team = team;
            this.location = location;
            this.material = material;
            this.timer = timer;
            this.base = base;
            this.layout = layout;
            this.displayId = displayId;
        }

        ForgeLayout layout() {
            return layout;
        }

        String displayId() {
            return displayId;
        }

        boolean spreads() {
            return material == Material.IRON_INGOT && layout.shape() != ForgeLayout.Shape.SINGLE;
        }

        int nextDrop() {
            return drops++;
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
