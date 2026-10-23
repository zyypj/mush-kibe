package br.com.muxe.core.common.account.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountProblems;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryAccountRepository;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordLoginHandlerTest {

    private final MutableClock clock = MutableClock.startingAt("2026-01-01T00:00:00Z");
    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final RecordLoginHandler handler = new RecordLoginHandler(accounts, transactions, clock);

    @Test
    void createsTheAccountOnFirstLogin() {
        UUID id = UUID.randomUUID();

        Result<Account> result = handler.handle(new RecordLoginCommand(id, "Steve"));

        Account account = result.orElseThrow();
        assertThat(account.firstSeenAt()).isEqualTo(clock.instant());
        assertThat(accounts.findById(id)).contains(account);
        assertThat(transactions.operations()).containsExactly("account.record_login");
    }

    @Test
    void updatesTheLastLoginOfReturningPlayers() {
        UUID id = UUID.randomUUID();
        handler.handle(new RecordLoginCommand(id, "Steve"));
        clock.advance(Duration.ofDays(1));

        Account account = handler.handle(new RecordLoginCommand(id, "Steve")).orElseThrow();

        assertThat(account.lastSeenAt()).isEqualTo(clock.instant());
        assertThat(account.firstSeenAt()).isEqualTo(clock.instant().minus(Duration.ofDays(1)));
    }

    @Test
    void refusesANameThatDiffersOnlyByCase() {
        handler.handle(new RecordLoginCommand(UUID.randomUUID(), "Steve"));

        Result<Account> result = handler.handle(new RecordLoginCommand(UUID.randomUUID(), "STEVE"));

        assertThat(result).isInstanceOfSatisfying(Result.Failure.class, failure ->
            assertThat(failure.problem().code()).isEqualTo(AccountProblems.NAME_CASE_CONFLICT));
        assertThat(accounts.size()).isEqualTo(1);
    }

    @Test
    void refusesInvalidNamesWithoutTouchingTheDatabase() {
        Result<Account> result = handler.handle(new RecordLoginCommand(UUID.randomUUID(), "no spaces!"));

        assertThat(result.isFailure()).isTrue();
        assertThat(transactions.operations()).isEmpty();
    }
}
