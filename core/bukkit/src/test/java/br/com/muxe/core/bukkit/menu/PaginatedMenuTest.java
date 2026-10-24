package br.com.muxe.core.bukkit.menu;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.util.List;
import java.util.stream.IntStream;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PaginatedMenuTest {

    private final ItemBuilder previous = ItemBuilder.of(Material.ARROW).name("&aPágina anterior");
    private final ItemBuilder next = ItemBuilder.of(Material.ARROW).name("&aPróxima página");

    @Test
    void fewItemsFitOnOnePageCenteredWithoutArrows() {
        Menu page = menuOf(4).page(1);

        assertThat(page.title()).isEqualTo("Selecionar lobby (1/1)");
        assertThat(page.size()).isEqualTo(27);
        assertThat(page.buttons().keySet()).containsExactlyInAnyOrder(11, 12, 13, 14);
    }

    @Test
    void manyItemsSpreadOverPagesWithArrows() {
        PaginatedMenu menu = menuOf(25);

        Menu first = menu.page(1);
        Menu second = menu.page(2);

        assertThat(first.title()).isEqualTo("Selecionar lobby (1/2)");
        assertThat(first.buttons()).hasSize(MenuLayout.ITEMS_PER_PAGE + 1);
        assertThat(first.button(first.size() - 1).orElseThrow().icon()).isEqualTo(next);
        assertThat(second.title()).isEqualTo("Selecionar lobby (2/2)");
        assertThat(second.button(second.size() - MenuLayout.ROW_WIDTH).orElseThrow().icon()).isEqualTo(previous);
        assertThat(second.buttons()).hasSize(4 + 1);
    }

    @Test
    void pagesOutOfRangeShowTheNearestPage() {
        assertThat(menuOf(25).page(9).title()).isEqualTo("Selecionar lobby (2/2)");
        assertThat(menuOf(0).page(1).buttons()).isEmpty();
    }

    @Test
    void fixedSlotsReplaceTheCenteredLayout() {
        PaginatedMenu menu = PaginatedMenu.builder("Lista").items(buttons(3)).slots(List.of(0, 2, 4)).rows(1).build();

        assertThat(menu.page(1).buttons().keySet()).containsExactlyInAnyOrder(0, 2, 4);
    }

    private PaginatedMenu menuOf(int count) {
        return PaginatedMenu.builder("Selecionar lobby ({page}/{pages})")
            .items(buttons(count))
            .previous(previous)
            .next(next)
            .build();
    }

    private static List<MenuButton> buttons(int count) {
        return IntStream.range(0, count).mapToObj(index -> MenuButton.of(ItemBuilder.of(Material.PAPER).name("#" + index))).toList();
    }
}
