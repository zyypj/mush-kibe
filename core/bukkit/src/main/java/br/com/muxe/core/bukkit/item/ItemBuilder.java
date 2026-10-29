package br.com.muxe.core.bukkit.item;

import br.com.muxe.core.bukkit.nms.SkullTextures;
import br.com.muxe.core.bukkit.text.Colors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class ItemBuilder {

    public static final short PLAYER_SKULL = 3;
    public static final int MAX_AMOUNT = 64;

    private final Material material;
    private final short data;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final boolean glowing;
    private final boolean unbreakable;
    private final boolean hideFlags;
    private final Color color;
    private final String skullOwner;
    private final String skullTexture;

    private ItemBuilder(
        Material material,
        short data,
        int amount,
        String name,
        List<String> lore,
        Map<Enchantment, Integer> enchantments,
        boolean glowing,
        boolean unbreakable,
        boolean hideFlags,
        Color color,
        String skullOwner,
        String skullTexture
    ) {
        this.material = Objects.requireNonNull(material, "material");
        if (amount < 1 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("Item amounts go from 1 to 64, got " + amount);
        }
        this.data = data;
        this.amount = amount;
        this.name = name == null ? null : Colors.translate(name);
        this.lore = lore == null ? List.of() : lore.stream().map(Colors::translate).toList();
        this.enchantments = enchantments == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(enchantments));
        this.glowing = glowing;
        this.unbreakable = unbreakable;
        this.hideFlags = hideFlags;
        this.color = color;
        this.skullOwner = skullOwner;
        this.skullTexture = SkullTexture.value(skullTexture);
    }

    public static ItemBuilder of(Material material) {
        return of(material, 0);
    }

    public static ItemBuilder of(Material material, int data) {
        return new ItemBuilder(material, (short) data, 1, null, List.of(), Map.of(), false, false, true, null, null, null);
    }

    public static ItemBuilder skull() {
        return of(Material.SKULL_ITEM, PLAYER_SKULL);
    }

    public static ItemBuilder from(ConfigurationSection section) {
        String materialName = section.getString("material", "");
        Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
        if (material == null) {
            throw new IllegalArgumentException("Unknown material '" + materialName + "' at " + section.getCurrentPath());
        }
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String key : enchantSection.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByName(key.toUpperCase(Locale.ROOT));
                if (enchantment == null) {
                    throw new IllegalArgumentException("Unknown enchantment '" + key + "' at " + section.getCurrentPath());
                }
                enchantments.put(enchantment, enchantSection.getInt(key, 1));
            }
        }
        String color = section.getString("color");
        return new ItemBuilder(
            material,
            (short) section.getInt("data", 0),
            section.getInt("amount", 1),
            section.getString("name"),
            section.getStringList("lore"),
            enchantments,
            section.getBoolean("glow", false),
            section.getBoolean("unbreakable", false),
            section.getBoolean("hide-flags", true),
            color == null ? null : parseColor(color),
            section.getString("skull-owner"),
            section.getString("skull-texture")
        );
    }

    public static Color parseColor(String value) {
        String text = value.trim();
        try {
            if (text.startsWith("#") && text.length() == 7) {
                return Color.fromRGB(Integer.parseInt(text.substring(1), 16));
            }
            String[] parts = text.split("\\s*,\\s*");
            if (parts.length == 3) {
                return Color.fromRGB(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Colors look like #RRGGBB or r, g, b, got: " + value, exception);
        }
        throw new IllegalArgumentException("Colors look like #RRGGBB or r, g, b, got: " + value);
    }

    public Material material() {
        return material;
    }

    public short data() {
        return data;
    }

    public int amount() {
        return amount;
    }

    public String name() {
        return name;
    }

    public List<String> lore() {
        return lore;
    }

    public Map<Enchantment, Integer> enchantments() {
        return enchantments;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public boolean hidesFlags() {
        return hideFlags;
    }

    public Color color() {
        return color;
    }

    public String skullOwner() {
        return skullOwner;
    }

    public String skullTexture() {
        return skullTexture;
    }

    public ItemBuilder material(Material value) {
        return new ItemBuilder(value, data, amount, name, lore, enchantments, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder data(int value) {
        return new ItemBuilder(material, (short) value, amount, name, lore, enchantments, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder amount(int value) {
        return new ItemBuilder(material, data, value, name, lore, enchantments, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder name(String value) {
        return new ItemBuilder(material, data, amount, value, lore, enchantments, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder lore(String... lines) {
        return lore(List.of(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        return new ItemBuilder(material, data, amount, name, lines, enchantments, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder addLore(String... lines) {
        List<String> merged = new ArrayList<>(lore);
        merged.addAll(List.of(lines));
        return lore(merged);
    }

    public ItemBuilder addLoreIf(boolean condition, String... lines) {
        return condition ? addLore(lines) : this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        Map<Enchantment, Integer> merged = new LinkedHashMap<>(enchantments);
        merged.put(enchantment, level);
        return new ItemBuilder(material, data, amount, name, lore, merged, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder glow() {
        return glow(true);
    }

    public ItemBuilder glow(boolean value) {
        return new ItemBuilder(material, data, amount, name, lore, enchantments, value, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder unbreakable() {
        return new ItemBuilder(material, data, amount, name, lore, enchantments, glowing, true, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemBuilder showFlags() {
        return new ItemBuilder(material, data, amount, name, lore, enchantments, glowing, unbreakable, false, color, skullOwner, skullTexture);
    }

    public ItemBuilder color(Color value) {
        return new ItemBuilder(material, data, amount, name, lore, enchantments, glowing, unbreakable, hideFlags, value, skullOwner, skullTexture);
    }

    public ItemBuilder skullOwner(String owner) {
        return new ItemBuilder(material, data, amount, name, lore, enchantments, glowing, unbreakable, hideFlags, color, owner, skullTexture);
    }

    public ItemBuilder skullTexture(String texture) {
        return new ItemBuilder(material, data, amount, name, lore, enchantments, glowing, unbreakable, hideFlags, color, skullOwner, texture);
    }

    public ItemBuilder replacing(String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders need name and value pairs");
        }
        return new ItemBuilder(material, data, amount, replace(name, placeholders), lore.stream().map(line -> replace(line, placeholders)).toList(),
            enchantments, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount, data);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (name != null) {
            meta.setDisplayName(name);
        }
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        enchantments.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
        if (glowing && enchantments.isEmpty()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (hideFlags) {
            meta.addItemFlags(ItemFlag.values());
        }
        if (unbreakable) {
            meta.spigot().setUnbreakable(true);
        }
        if (color != null && meta instanceof LeatherArmorMeta leather) {
            leather.setColor(color);
        }
        if (meta instanceof SkullMeta skull) {
            if (skullTexture != null) {
                SkullTextures.apply(skull, skullTexture);
            } else if (skullOwner != null) {
                skull.setOwner(skullOwner);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemBuilder item)) {
            return false;
        }
        return material == item.material && data == item.data && amount == item.amount && glowing == item.glowing
            && unbreakable == item.unbreakable && hideFlags == item.hideFlags && Objects.equals(name, item.name)
            && lore.equals(item.lore) && enchantments.equals(item.enchantments) && Objects.equals(color, item.color)
            && Objects.equals(skullOwner, item.skullOwner) && Objects.equals(skullTexture, item.skullTexture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, data, amount, name, lore, enchantments, glowing, unbreakable, hideFlags, color, skullOwner, skullTexture);
    }

    @Override
    public String toString() {
        return "ItemBuilder[" + material + ":" + data + " x" + amount + (name == null ? "" : " " + name) + "]";
    }

    private static String replace(String text, String... placeholders) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (int index = 0; index < placeholders.length; index += 2) {
            result = result.replace("{" + placeholders[index] + "}", placeholders[index + 1]);
        }
        return result;
    }
}
