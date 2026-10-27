package br.com.muxe.core.bukkit.placement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReconcilerTest {

    private record Item(String id, String text) {
    }

    private final Reconciler<Item> reconciler = new Reconciler<>(Item::id);

    @Test
    void everythingIsNewTheFirstTime() {
        Reconciler.Changes<Item> changes = reconciler.next(List.of(new Item("a", "1"), new Item("b", "2")));

        assertThat(changes.put()).extracting(Item::id).containsExactly("a", "b");
        assertThat(changes.removed()).isEmpty();
    }

    @Test
    void onlyChangedItemsAreShownAgainAndMissingOnesAreRemoved() {
        reconciler.next(List.of(new Item("a", "1"), new Item("b", "2"), new Item("c", "3")));

        Reconciler.Changes<Item> changes = reconciler.next(List.of(new Item("a", "1"), new Item("b", "changed"), new Item("d", "4")));

        assertThat(changes.put()).extracting(Item::id).containsExactly("b", "d");
        assertThat(changes.removed()).containsExactly("c");
        assertThat(reconciler.all()).extracting(Item::id).containsExactly("a", "b", "d");
    }
}
