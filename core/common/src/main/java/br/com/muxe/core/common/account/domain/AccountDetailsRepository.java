package br.com.muxe.core.common.account.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountDetailsRepository {

    Optional<AccountDetails> findById(UUID id);

    Optional<AccountDetails> findByUsername(String username);

    void saveLocation(UUID id, AccountLocation location, Instant at);
}
