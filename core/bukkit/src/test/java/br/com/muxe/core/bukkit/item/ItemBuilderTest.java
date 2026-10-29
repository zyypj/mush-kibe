package br.com.muxe.core.bukkit.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ItemBuilderTest {

    @Test
    void buildsFluentlyWithoutChangingTheOriginal() {
        ItemBuilder base = ItemBuilder.of(Material.NETHER_STAR);

        ItemBuilder star = base.name("&aSelecionar lobby").lore("&7Clique para abrir").amount(2).glow();

        assertThat(base.name()).isNull();
        assertThat(base.isGlowing()).isFalse();
        assertThat(star.name()).isEqualTo("§aSelecionar lobby");
        assertThat(star.lore()).containsExactly("§7Clique para abrir");
        assertThat(star.amount()).isEqualTo(2);
        assertThat(star.isGlowing()).isTrue();
        assertThat(star.hidesFlags()).isTrue();
    }

    @Test
    void loreCanGrowAndBeConditional() {
        ItemBuilder item = ItemBuilder.of(Material.PAPER)
            .lore("&7primeira")
            .addLore("&7segunda", "&7terceira")
            .addLoreIf(false, "&cnunca")
            .addLoreIf(true, "&asempre");

        assertThat(item.lore()).containsExactly("§7primeira", "§7segunda", "§7terceira", "§asempre");
    }

    @Test
    void placeholdersAreReplacedInNameAndLore() {
        ItemBuilder pane = ItemBuilder.of(Material.STAINED_GLASS_PANE, 5).name("&aLobby Principal #{number}").lore("&fJogadores &a{online}/{max}");

        ItemBuilder filled = pane.replacing("number", "3", "online", "32", "max", "60");

        assertThat(filled.name()).isEqualTo("§aLobby Principal #3");
        assertThat(filled.lore()).containsExactly("§fJogadores §a32/60");
        assertThat(filled.data()).isEqualTo((short) 5);
    }

    @Test
    void readsEverythingFromConfiguration() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n",
            "item:",
            "  material: leather_chestplate",
            "  amount: 1",
            "  name: \"&cPeitoral\"",
            "  lore:",
            "    - \"&7linha\"",
            "  glow: true",
            "  unbreakable: true",
            "  hide-flags: false",
            "  color: \"#FF5555\"",
            "  skull-owner: Notch"
        ));

        ItemBuilder item = ItemBuilder.from(config.getConfigurationSection("item"));

        assertThat(item.material()).isEqualTo(Material.LEATHER_CHESTPLATE);
        assertThat(item.name()).isEqualTo("§cPeitoral");
        assertThat(item.lore()).containsExactly("§7linha");
        assertThat(item.isGlowing()).isTrue();
        assertThat(item.isUnbreakable()).isTrue();
        assertThat(item.hidesFlags()).isFalse();
        assertThat(item.color()).isEqualTo(Color.fromRGB(0xFF, 0x55, 0x55));
        assertThat(item.skullOwner()).isEqualTo("Notch");
    }

    @Test
    void skullsCanUseAnOwnerOrATexture() {
        assertThat(ItemBuilder.skull().skullOwner("Notch").material()).isEqualTo(Material.SKULL_ITEM);
        assertThat(ItemBuilder.skull().data()).isEqualTo((short) 3);
        assertThat(ItemBuilder.skull().skullTexture("dGV4dHVyZQ==").skullTexture()).isEqualTo("dGV4dHVyZQ==");
    }

    @Test
    void invalidInputIsRejected() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("item:\n  material: NOT_A_BLOCK\n");

        assertThatThrownBy(() -> ItemBuilder.from(config.getConfigurationSection("item"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ItemBuilder.of(Material.STONE).amount(65)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ItemBuilder.parseColor("roxo")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void colorsAcceptHexOrRgb() {
        assertThat(ItemBuilder.parseColor("#55FF55")).isEqualTo(Color.fromRGB(0x55, 0xFF, 0x55));
        assertThat(ItemBuilder.parseColor("85, 255, 255")).isEqualTo(Color.fromRGB(85, 255, 255));
    }
}
