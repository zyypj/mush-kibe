package br.com.muxe.core.bukkit.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class MenuTest {

    private final ItemBuilder glass = ItemBuilder.of(Material.STAINED_GLASS_PANE, 15).name(" ");
    private final ItemBuilder star = ItemBuilder.of(Material.NETHER_STAR).name("&aEstrela");

    @Test
    void buttonsGoBySlotOrByRowAndColumn() {
        Menu menu = Menu.builder("Teste").rows(3)
            .button(0, star)
            .button(1, 4, star, MenuClick.NONE)
            .build();

        assertThat(menu.button(0)).isPresent();
        assertThat(menu.button(13)).isPresent();
        assertThat(menu.button(1)).isEmpty();
        assertThat(menu.size()).isEqualTo(27);
    }

    @Test
    void theBorderSurroundsTheMenuWithoutCoveringButtons() {
        Menu menu = Menu.builder("Borda").rows(3).border(glass).button(0, star).build();

        assertThat(menu.button(0).orElseThrow().icon()).isEqualTo(star);
        assertThat(menu.button(8).orElseThrow().icon()).isEqualTo(glass);
        assertThat(menu.button(9).orElseThrow().icon()).isEqualTo(glass);
        assertThat(menu.button(17).orElseThrow().icon()).isEqualTo(glass);
        assertThat(menu.button(13)).isEmpty();
        assertThat(Slots.border(3)).hasSize(20);
    }

    @Test
    void fillCoversOnlyTheEmptySlots() {
        Menu menu = Menu.builder("Cheio").rows(1).button(4, star).fill(glass).build();

        assertThat(menu.buttons()).hasSize(9);
        assertThat(menu.button(4).orElseThrow().icon()).isEqualTo(star);
    }

    @Test
    void titlesAreTranslatedAndTrimmedToTheProtocolLimit() {
        assertThat(Menu.builder("&aSelecionar").build().title()).isEqualTo("§aSelecionar");
        assertThat(Menu.builder("x".repeat(40)).build().title()).hasSize(Menu.MAX_TITLE);
    }

    @Test
    void slotsOutsideTheMenuAreRejected() {
        assertThatThrownBy(() -> Menu.builder("x").rows(1).button(9, star)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Menu.builder("x").rows(7)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Slots.of(0, 9)).isInstanceOf(IllegalArgumentException.class);
    }
}
