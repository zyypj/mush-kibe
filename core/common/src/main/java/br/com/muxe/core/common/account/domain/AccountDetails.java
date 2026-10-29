package br.com.muxe.core.common.account.domain;

import java.util.Objects;
import java.util.Optional;

public record AccountDetails(Account account, long number, Optional<AccountLocation> location) {

    public AccountDetails {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(location, "location");
    }
}
