package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.bedwars.room.RoomTeam.Upgrade;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class Kits {

    public static final List<Material> SWORDS = List.of(Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD);

    private static final Material[] BOOTS = {Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS};
    private static final Material[] LEGGINGS = {Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS};
    private static final Material[] PICKAXES = {Material.WOOD_PICKAXE, Material.IRON_PICKAXE, Material.GOLD_PICKAXE, Material.DIAMOND_PICKAXE};
    private static final int[][] PICKAXE_ENCHANTS = {{1, 0}, {2, 0}, {3, 2}, {3, 0}};
    private static final Material[] AXES = {Material.WOOD_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE};
    private static final int[] AXE_EFFICIENCY = {1, 1, 2, 3};
    private static final int INFINITE = Integer.MAX_VALUE;

    private Kits() {
    }

    public static void spawnKit(Player player, RoomPlayer roomPlayer, RoomTeam team) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        armor(player, roomPlayer, team);
        inventory.addItem(sword(Material.WOOD_SWORD, team));
        if (roomPlayer.hasShears()) {
            inventory.addItem(unbreakable(new ItemStack(Material.SHEARS)));
        }
        if (roomPlayer.pickaxeTier() > 0) {
            inventory.addItem(pickaxe(roomPlayer.pickaxeTier()));
        }
        if (roomPlayer.axeTier() > 0) {
            inventory.addItem(axe(roomPlayer.axeTier(), team));
        }
        effects(player, team);
    }

    public static void armor(Player player, RoomPlayer roomPlayer, RoomTeam team) {
        PlayerInventory inventory = player.getInventory();
        int protection = team.level(Upgrade.PROTECTION);
        inventory.setHelmet(enchant(leather(Material.LEATHER_HELMET, team.color()), Enchantment.PROTECTION_ENVIRONMENTAL, protection));
        inventory.setChestplate(enchant(leather(Material.LEATHER_CHESTPLATE, team.color()), Enchantment.PROTECTION_ENVIRONMENTAL, protection));
        int tier = Math.clamp(roomPlayer.armorTier(), 0, BOOTS.length - 1);
        ItemStack leggings = tier == 0 ? leather(LEGGINGS[0], team.color()) : unbreakable(new ItemStack(LEGGINGS[tier]));
        ItemStack boots = tier == 0 ? leather(BOOTS[0], team.color()) : unbreakable(new ItemStack(BOOTS[tier]));
        inventory.setLeggings(enchant(leggings, Enchantment.PROTECTION_ENVIRONMENTAL, protection));
        inventory.setBoots(enchant(boots, Enchantment.PROTECTION_ENVIRONMENTAL, protection));
    }

    public static void effects(Player player, RoomTeam team) {
        int haste = team.level(Upgrade.HASTE);
        player.removePotionEffect(PotionEffectType.FAST_DIGGING);
        if (haste > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, INFINITE, haste - 1, true, false));
        }
    }

    public static void sharpen(Player player, RoomTeam team) {
        int sharpness = team.level(Upgrade.SHARPNESS);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (SWORDS.contains(item.getType()) || List.of(AXES).contains(item.getType()))) {
                enchant(item, Enchantment.DAMAGE_ALL, sharpness);
            }
        }
    }

    public static void protect(Player player, RoomTeam team) {
        int protection = team.level(Upgrade.PROTECTION);
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                enchant(item, Enchantment.PROTECTION_ENVIRONMENTAL, protection);
            }
        }
    }

    public static ItemStack sword(Material material, RoomTeam team) {
        return enchant(unbreakable(new ItemStack(material)), Enchantment.DAMAGE_ALL, team.level(Upgrade.SHARPNESS));
    }

    public static ItemStack pickaxe(int tier) {
        int index = Math.clamp(tier, 1, PICKAXES.length) - 1;
        ItemStack item = unbreakable(new ItemStack(PICKAXES[index]));
        enchant(item, Enchantment.DIG_SPEED, PICKAXE_ENCHANTS[index][0]);
        return enchant(item, Enchantment.DAMAGE_ALL, PICKAXE_ENCHANTS[index][1]);
    }

    public static ItemStack axe(int tier, RoomTeam team) {
        int index = Math.clamp(tier, 1, AXES.length) - 1;
        ItemStack item = enchant(unbreakable(new ItemStack(AXES[index])), Enchantment.DIG_SPEED, AXE_EFFICIENCY[index]);
        return enchant(item, Enchantment.DAMAGE_ALL, team.level(Upgrade.SHARPNESS));
    }

    public static boolean isPickaxe(Material material) {
        return List.of(PICKAXES).contains(material);
    }

    public static boolean isAxe(Material material) {
        return List.of(AXES).contains(material);
    }

    public static ItemStack wool(TeamColor color, int amount) {
        return new ItemStack(Material.WOOL, amount, (short) color.woolData());
    }

    static ItemStack leather(Material material, TeamColor color) {
        ItemStack item = unbreakable(new ItemStack(material));
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(color.rgb()));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack unbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        if (level > 0) {
            item.addUnsafeEnchantment(enchantment, level);
        } else {
            item.removeEnchantment(enchantment);
        }
        return item;
    }
}
