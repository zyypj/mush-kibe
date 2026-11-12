package br.com.muxe.minigame.bedwars.game;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record RandomItems(Set<String> modes, Duration every, double shopChance, int maxAmount, Set<Material> excluded) {

    public static final RandomItems NONE = new RandomItems(Set.of(), Duration.ofSeconds(5), 0.5, 4, Set.of());
    public static final Set<Material> NOT_ITEMS = EnumSet.of(
        Material.AIR, Material.WATER, Material.STATIONARY_WATER, Material.LAVA, Material.STATIONARY_LAVA, Material.BED_BLOCK,
        Material.PISTON_EXTENSION, Material.PISTON_MOVING_PIECE, Material.DOUBLE_STEP, Material.FIRE, Material.REDSTONE_WIRE, Material.CROPS,
        Material.SOIL, Material.BURNING_FURNACE, Material.SIGN_POST, Material.WOODEN_DOOR, Material.WALL_SIGN, Material.IRON_DOOR_BLOCK,
        Material.GLOWING_REDSTONE_ORE, Material.REDSTONE_TORCH_OFF, Material.SUGAR_CANE_BLOCK, Material.PORTAL, Material.CAKE_BLOCK,
        Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.PUMPKIN_STEM, Material.MELON_STEM, Material.NETHER_WARTS,
        Material.BREWING_STAND, Material.CAULDRON, Material.ENDER_PORTAL, Material.REDSTONE_LAMP_ON, Material.WOOD_DOUBLE_STEP, Material.COCOA,
        Material.TRIPWIRE, Material.FLOWER_POT, Material.CARROT, Material.POTATO, Material.SKULL, Material.REDSTONE_COMPARATOR_OFF,
        Material.REDSTONE_COMPARATOR_ON, Material.STANDING_BANNER, Material.WALL_BANNER, Material.DAYLIGHT_DETECTOR_INVERTED,
        Material.DOUBLE_STONE_SLAB2, Material.SPRUCE_DOOR, Material.BIRCH_DOOR, Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR
    );

    public RandomItems {
        modes = Set.copyOf(modes);
        excluded = excluded.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(excluded));
        if (every.isNegative() || every.isZero()) {
            throw new IllegalArgumentException("Random items come at a positive interval, got: " + every);
        }
        if (shopChance < 0 || shopChance > 1 || maxAmount < 1) {
            throw new IllegalArgumentException("Random items need a shop chance from 0 to 1 and a maximum amount of at least 1");
        }
    }

    public static RandomItems from(ConfigurationSection section) {
        if (section == null) {
            return NONE;
        }
        Set<Material> excluded = EnumSet.noneOf(Material.class);
        for (String name : section.getStringList("excluded")) {
            Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (material == null) {
                throw new IllegalArgumentException("Unknown material '" + name + "' in random-items.excluded");
            }
            excluded.add(material);
        }
        return new RandomItems(Set.copyOf(section.getStringList("modes")), Duration.ofMillis(Math.round(section.getDouble("every-seconds", 5) * 1000)),
            section.getDouble("shop-chance", 0.5), section.getInt("max-amount", 4), excluded);
    }

    public boolean in(String mode) {
        return modes.contains(mode);
    }

    public List<Material> pool() {
        return Arrays.stream(Material.values()).filter(material -> !NOT_ITEMS.contains(material) && !excluded.contains(material)).toList();
    }

    public Draw draw(RandomGenerator random, List<String> shopItems) {
        if (!shopItems.isEmpty() && random.nextDouble() < shopChance) {
            return new ShopDraw(shopItems.get(random.nextInt(shopItems.size())));
        }
        return game(random);
    }

    public GameDraw game(RandomGenerator random) {
        List<Material> pool = pool();
        Material material = pool.get(random.nextInt(pool.size()));
        int most = Math.min(maxAmount, material.getMaxStackSize());
        return new GameDraw(material, most <= 1 ? 1 : 1 + random.nextInt(most));
    }

    public sealed interface Draw permits ShopDraw, GameDraw {
    }

    public record ShopDraw(String itemId) implements Draw {
    }

    public record GameDraw(Material material, int amount) implements Draw {
    }
}
