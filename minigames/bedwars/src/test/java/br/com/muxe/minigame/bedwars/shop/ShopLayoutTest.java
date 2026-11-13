package br.com.muxe.minigame.bedwars.shop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShopLayoutTest {

    @Test
    void aFewItemsSitInTheMiddleOfTheMiddleRow() {
        assertThat(ShopLayout.centered(1)).containsExactly(31);
        assertThat(ShopLayout.centered(3)).containsExactly(30, 31, 32);
        assertThat(ShopLayout.centered(5)).containsExactly(29, 30, 31, 32, 33);
        assertThat(ShopLayout.centered(7)).containsExactly(28, 29, 30, 31, 32, 33, 34);
    }

    @Test
    void anEvenNumberOfItemsLeavesTheMiddleSlotEmptySoBothSidesMatch() {
        assertThat(ShopLayout.centered(2)).containsExactly(30, 32);
        assertThat(ShopLayout.centered(4)).containsExactly(29, 30, 32, 33);
        assertThat(ShopLayout.centered(6)).containsExactly(28, 29, 30, 32, 33, 34);
    }

    @Test
    void moreThanARowFillsRowsOfSevenAndCentresTheLastOne() {
        assertThat(ShopLayout.centered(9)).containsExactly(19, 20, 21, 22, 23, 24, 25, 30, 32);
        assertThat(ShopLayout.centered(21)).hasSize(21).startsWith(19).endsWith(43);
        assertThat(ShopLayout.centered(30)).hasSize(21);
        assertThat(ShopLayout.centered(0)).isEmpty();
    }
}
