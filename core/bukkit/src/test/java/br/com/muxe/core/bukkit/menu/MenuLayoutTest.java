package br.com.muxe.core.bukkit.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MenuLayoutTest {

    @Test
    void fourItemsAreCenteredOnTheSecondRow() {
        assertThat(MenuLayout.centered(4, 1)).containsExactly(11, 12, 13, 14);
    }

    @Test
    void twoItemsLeanToTheLeftOfTheCenter() {
        assertThat(MenuLayout.centered(2, 1)).containsExactly(12, 13);
    }

    @Test
    void oneItemSitsInTheMiddle() {
        assertThat(MenuLayout.centered(1, 1)).containsExactly(13);
    }

    @Test
    void moreThanSevenItemsWrapToTheNextRowCentered() {
        List<Integer> slots = MenuLayout.centered(9, 1);

        assertThat(slots).startsWith(10, 11, 12, 13, 14, 15, 16).endsWith(21, 22);
    }

    @Test
    void theInventoryHasARowAboveAndBelowTheItems() {
        assertThat(MenuLayout.rowsFor(4)).isEqualTo(3);
        assertThat(MenuLayout.rowsFor(0)).isEqualTo(3);
        assertThat(MenuLayout.rowsFor(8)).isEqualTo(4);
        assertThat(MenuLayout.rowsFor(21)).isEqualTo(5);
    }

    @Test
    void pagesSplitTheItemsAndClampTheNumber() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);

        Page<Integer> second = Page.of(items, 2, 2);
        Page<Integer> beyond = Page.of(items, 2, 9);
        Page<Integer> empty = Page.of(List.<Integer>of(), 2, 1);

        assertThat(second.items()).containsExactly(3, 4);
        assertThat(second.number()).isEqualTo(2);
        assertThat(second.total()).isEqualTo(3);
        assertThat(beyond.items()).containsExactly(5);
        assertThat(beyond.number()).isEqualTo(3);
        assertThat(empty.items()).isEmpty();
        assertThat(empty.total()).isEqualTo(1);
        assertThat(second.hasPrevious()).isTrue();
        assertThat(second.hasNext()).isTrue();
        assertThat(beyond.hasNext()).isFalse();
    }
}
