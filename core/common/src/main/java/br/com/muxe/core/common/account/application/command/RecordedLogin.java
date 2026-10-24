package br.com.muxe.core.common.account.application.command;

import br.com.muxe.core.common.account.domain.Account;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RecordedLogin(Account account, Optional<UUID> nameReclaimedFrom) {

    public RecordedLogin {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(nameReclaimedFrom, "nameReclaimedFrom");
    }

    public static RecordedLogin of(Account account) {
        return new RecordedLogin(account, Optional.empty());
    }

    public static RecordedLogin reclaiming(Account account, UUID previousHolder) {
        return new RecordedLogin(account, Optional.of(previousHolder));
    }
}
