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

        Account account = login(id, "Steve", false);

        assertThat(account.firstSeenAt()).isEqualTo(clock.instant());
        assertThat(account.premium()).isFalse();
        assertThat(accounts.findById(id)).contains(account);
        assertThat(transactions.operations()).containsExactly("account.record_login");
    }

    @Test
    void remembersPremiumAccounts() {
        assertThat(login(UUID.randomUUID(), "Notch", true).premium()).isTrue();
    }

    @Test
    void keepsTheModeChosenOnTheFirstLogin() {
        UUID id = UUID.randomUUID();
        login(id, "Notch", true);

        assertThat(login(id, "Notch", false).premium()).isTrue();
    }

    @Test
    void updatesTheLastLoginOfReturningPlayers() {
        UUID id = UUID.randomUUID();
        login(id, "Steve", false);
        clock.advance(Duration.ofDays(1));

        Account account = login(id, "Steve", false);

        assertThat(account.lastSeenAt()).isEqualTo(clock.instant());
        assertThat(account.firstSeenAt()).isEqualTo(clock.instant().minus(Duration.ofDays(1)));
    }

    @Test
    void aPremiumPlayerWhoChangedNameKeepsTheSameAccount() {
        UUID id = UUID.randomUUID();
        login(id, "Steve", true);

        Account account = login(id, "Steve2", true);

        assertThat(account.username()).isEqualTo("Steve2");
        assertThat(accounts.findByUsername("Steve")).isEmpty();
        assertThat(accounts.size()).isEqualTo(1);
    }

    @Test
    void theNewPremiumOwnerOfANameTakesItFromThePreviousOwner() {
        UUID previousOwner = UUID.randomUUID();
        UUID newOwner = UUID.randomUUID();
        login(previousOwner, "Steve", true);

        RecordedLogin recorded = handler.handle(new RecordLoginCommand(newOwner, "Steve", true)).orElseThrow();

        assertThat(recorded.nameReclaimedFrom()).contains(previousOwner);
        assertThat(accounts.findByUsername("Steve")).map(Account::id).contains(newOwner);
        assertThat(accounts.findById(previousOwner)).hasValueSatisfying(previous -> {
            assertThat(previous.currentUsername()).isEmpty();
            assertThat(previous.premium()).isTrue();
        });
    }

    @Test
    void aPremiumPlayerTakesTheirNameBackFromACrackedAccountInAnyCase() {
        UUID cracked = UUID.randomUUID();
        UUID premium = UUID.randomUUID();
        login(cracked, "notch", false);

        RecordedLogin recorded = handler.handle(new RecordLoginCommand(premium, "Notch", true)).orElseThrow();

        assertThat(recorded.nameReclaimedFrom()).contains(cracked);
        assertThat(accounts.findById(cracked)).hasValueSatisfying(account -> assertThat(account.currentUsername()).isEmpty());
    }

    @Test
    void theReleasedAccountGetsANameAgainWhenItsOwnerReturns() {
        UUID previousOwner = UUID.randomUUID();
        login(previousOwner, "Steve", true);
        login(UUID.randomUUID(), "Steve", true);

        Account account = login(previousOwner, "SteveRenamed", true);

        assertThat(account.username()).isEqualTo("SteveRenamed");
    }

    @Test
    void aCrackedPlayerCannotTakeANameThatDiffersOnlyByCase() {
        login(UUID.randomUUID(), "Steve", false);

        Result<RecordedLogin> result = handler.handle(new RecordLoginCommand(UUID.randomUUID(), "STEVE", false));

        assertThat(result).isInstanceOfSatisfying(Result.Failure.class, failure ->
            assertThat(failure.problem().code()).isEqualTo(AccountProblems.NAME_CASE_CONFLICT));
        assertThat(accounts.size()).isEqualTo(1);
    }

    @Test
    void aCrackedPlayerCannotTakeAPremiumName() {
        login(UUID.randomUUID(), "Notch", true);

        Result<RecordedLogin> result = handler.handle(new RecordLoginCommand(UUID.randomUUID(), "notch", false));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void refusesInvalidNamesWithoutTouchingTheDatabase() {
        Result<RecordedLogin> result = handler.handle(new RecordLoginCommand(UUID.randomUUID(), "no spaces!", false));

        assertThat(result.isFailure()).isTrue();
        assertThat(transactions.operations()).isEmpty();
    }

    private Account login(UUID id, String username, boolean premium) {
        return handler.handle(new RecordLoginCommand(id, username, premium)).orElseThrow().account();
    }
}
