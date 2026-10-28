package br.com.muxe.core.common.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.account.application.command.SaveAccountLocationCommand;
import br.com.muxe.core.common.account.application.command.SaveAccountLocationHandler;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsHandler;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsQuery;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryAccountDetailsRepository;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountDetailsHandlersTest {

    private final InMemoryAccountDetailsRepository details = new InMemoryAccountDetailsRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final FindAccountDetailsHandler find = new FindAccountDetailsHandler(details, transactions);
    private final UUID player = UUID.randomUUID();

    @Test
    void anAccountIsFoundByIdOrNameWithItsLocation() {
        details.add(Account.firstLogin(player, "tadeu", true, Instant.parse("2021-09-10T19:29:25Z")));
        AccountLocation location = new AccountLocation(Optional.of("Brasil"), Optional.of(" Minas Gerais "), Optional.of(""), Optional.empty());

        new SaveAccountLocationHandler(details, transactions, new MutableClock(Instant.parse("2026-10-28T16:07:27Z")))
            .handle(new SaveAccountLocationCommand(player, location));

        assertThat(find.handle(FindAccountDetailsQuery.byUsername("Tadeu"))).hasValueSatisfying(found -> {
            assertThat(found.number()).isEqualTo(1);
            assertThat(found.location()).contains(new AccountLocation(Optional.of("Brasil"), Optional.of("Minas Gerais"), Optional.empty(), Optional.empty()));
        });
        assertThat(find.handle(FindAccountDetailsQuery.byId(UUID.randomUUID()))).isEmpty();
        assertThat(transactions.operations()).contains("account.save_location", "account.find_details");
    }

    @Test
    void aLookupUsesExactlyOneKey() {
        assertThatThrownBy(() -> new FindAccountDetailsQuery(Optional.empty(), Optional.empty())).isInstanceOf(IllegalArgumentException.class);
        assertThat(AccountLocation.UNKNOWN.isUnknown()).isTrue();
    }
}
