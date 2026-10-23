package br.com.muxe.core.proxy.login;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.account.application.query.FindAccountByUsernameHandler;
import br.com.muxe.core.common.account.application.query.FindAccountByUsernameQuery;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryAccountRepository;
import br.com.muxe.core.proxy.premium.PremiumStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LoginModeResolverTest {

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
    private final AtomicInteger lookups = new AtomicInteger();

    LoginModeResolverTest() {
        queries.register(FindAccountByUsernameQuery.class, new FindAccountByUsernameHandler(accounts, new DirectTransactions()));
    }

    @Test
    void premiumAccountsAlwaysLogInThroughMojangWithoutALookup() {
        accounts.save(Account.firstLogin(UUID.randomUUID(), "Notch", true, Instant.EPOCH));

        assertThat(resolver(PremiumStatus.CRACKED, true).resolve("notch")).isEqualTo(LoginMode.PREMIUM);
        assertThat(lookups).hasValue(0);
    }

    @Test
    void crackedAccountsStayCrackedWhileTheNameIsNotPremium() {
        accounts.save(Account.firstLogin(UUID.randomUUID(), "Steve", false, Instant.EPOCH));

        assertThat(resolver(PremiumStatus.CRACKED, true).resolve("Steve")).isEqualTo(LoginMode.CRACKED);
        assertThat(resolver(PremiumStatus.UNKNOWN, true).resolve("Steve")).isEqualTo(LoginMode.CRACKED);
    }

    @Test
    void aCrackedNameThatBecamePremiumGoesToItsMojangOwner() {
        accounts.save(Account.firstLogin(UUID.randomUUID(), "Steve", false, Instant.EPOCH));

        assertThat(resolver(PremiumStatus.PREMIUM, true).resolve("Steve")).isEqualTo(LoginMode.PREMIUM);
    }

    @Test
    void newNamesFollowTheMojangLookup() {
        assertThat(resolver(PremiumStatus.PREMIUM, true).resolve("Jeb")).isEqualTo(LoginMode.PREMIUM);
        assertThat(resolver(PremiumStatus.CRACKED, true).resolve("Jeb")).isEqualTo(LoginMode.CRACKED);
    }

    @Test
    void anUnknownLookupLeavesNewNamesUndecided() {
        assertThat(resolver(PremiumStatus.UNKNOWN, true).resolve("Jeb")).isEqualTo(LoginMode.UNDECIDED);
    }

    @Test
    void everyNewNameIsCrackedWhenPremiumLoginIsDisabled() {
        accounts.save(Account.firstLogin(UUID.randomUUID(), "Steve", false, Instant.EPOCH));

        assertThat(resolver(PremiumStatus.PREMIUM, false).resolve("Jeb")).isEqualTo(LoginMode.CRACKED);
        assertThat(resolver(PremiumStatus.PREMIUM, false).resolve("Steve")).isEqualTo(LoginMode.CRACKED);
        assertThat(lookups).hasValue(0);
    }

    private LoginModeResolver resolver(PremiumStatus status, boolean premiumEnabled) {
        return new LoginModeResolver(queries, username -> {
            lookups.incrementAndGet();
            return status;
        }, premiumEnabled);
    }
}
