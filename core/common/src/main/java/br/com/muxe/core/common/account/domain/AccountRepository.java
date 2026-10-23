package br.com.muxe.core.common.account.domain;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findById(UUID id);

    Optional<Account> findByUsername(String username);

    void save(Account account);
}
