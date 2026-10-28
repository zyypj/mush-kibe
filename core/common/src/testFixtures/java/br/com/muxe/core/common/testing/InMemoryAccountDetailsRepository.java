package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountDetails;
import br.com.muxe.core.common.account.domain.AccountDetailsRepository;
import br.com.muxe.core.common.account.domain.AccountLocation;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryAccountDetailsRepository implements AccountDetailsRepository {

    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> numbers = new ConcurrentHashMap<>();
    private final Map<UUID, AccountLocation> locations = new ConcurrentHashMap<>();
    private final AtomicLong next = new AtomicLong();

    public void add(Account account) {
        accounts.put(account.id(), account);
        numbers.computeIfAbsent(account.id(), ignored -> next.incrementAndGet());
    }

    @Override
    public Optional<AccountDetails> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id)).map(this::details);
    }

    @Override
    public Optional<AccountDetails> findByUsername(String username) {
        return accounts.values().stream()
            .filter(account -> account.currentUsername().map(name -> name.equalsIgnoreCase(username)).orElse(false))
            .findFirst()
            .map(this::details);
    }

    @Override
    public void saveLocation(UUID id, AccountLocation location, Instant at) {
        locations.put(id, location);
    }

    private AccountDetails details(Account account) {
        return new AccountDetails(account, numbers.get(account.id()), Optional.ofNullable(locations.get(account.id())));
    }
}
