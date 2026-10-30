package br.com.muxe.core.bukkit.credit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CreditBalancesTest {

    private final Map<UUID, Long> stored = new HashMap<>();
    private final CreditBalances balances = new CreditBalances(id -> CompletableFuture.completedFuture(stored.getOrDefault(id, 0L)), Runnable::run);

    @Test
    void trackedPlayersShowTheStoredBalance() {
        UUID player = UUID.randomUUID();
        stored.put(player, 1_500L);

        balances.track(player);

        assertThat(balances.of(player)).isEqualTo(1_500L);
    }

    @Test
    void refreshingPicksUpChangesMadeElsewhere() {
        UUID player = UUID.randomUUID();
        balances.track(player);
        stored.put(player, 40L);

        balances.refreshAll();

        assertThat(balances.of(player)).isEqualTo(40L);
    }

    @Test
    void playersWhoLeftAreNotCachedAgain() {
        UUID player = UUID.randomUUID();
        stored.put(player, 10L);
        balances.track(player);
        balances.forget(player);

        balances.refresh(player);
        balances.set(player, 99L);

        assertThat(balances.of(player)).isZero();
    }
}
