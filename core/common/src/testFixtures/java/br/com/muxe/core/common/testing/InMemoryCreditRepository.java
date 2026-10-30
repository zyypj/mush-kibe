package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.credit.domain.CreditRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public final class InMemoryCreditRepository implements CreditRepository {

    private final Map<UUID, Long> balances = new HashMap<>();
    private final List<Change> changes = new ArrayList<>();

    @Override
    public long balance(UUID playerId) {
        return balances.getOrDefault(playerId, 0L);
    }

    @Override
    public long add(UUID playerId, long amount, String reason, Instant at) {
        long balance = balances.merge(playerId, amount, Long::sum);
        changes.add(new Change(playerId, amount, reason, at));
        return balance;
    }

    @Override
    public OptionalLong spend(UUID playerId, long amount, String reason, Instant at) {
        long balance = balance(playerId);
        if (balance < amount) {
            return OptionalLong.empty();
        }
        balances.put(playerId, balance - amount);
        changes.add(new Change(playerId, -amount, reason, at));
        return OptionalLong.of(balance - amount);
    }

    public List<Change> changes() {
        return List.copyOf(changes);
    }

    public record Change(UUID playerId, long amount, String reason, Instant at) {
    }
}
