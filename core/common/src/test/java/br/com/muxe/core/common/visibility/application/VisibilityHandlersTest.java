package br.com.muxe.core.common.visibility.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryVisibilityRepository;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.visibility.application.command.ChangeVisibilityCommand;
import br.com.muxe.core.common.visibility.application.command.ChangeVisibilityHandler;
import br.com.muxe.core.common.visibility.application.query.FindVisibilityHandler;
import br.com.muxe.core.common.visibility.application.query.FindVisibilityQuery;
import br.com.muxe.core.common.visibility.domain.Visibility;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisibilityHandlersTest {

    private final InMemoryVisibilityRepository repository = new InMemoryVisibilityRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-24T12:00:00Z"));
    private final ChangeVisibilityHandler change = new ChangeVisibilityHandler(repository, transactions, clock);
    private final FindVisibilityHandler find = new FindVisibilityHandler(repository, transactions);
    private final UUID player = UUID.randomUUID();

    @Test
    void playersWithoutASettingSeeEveryone() {
        assertThat(find.handle(new FindVisibilityQuery(player))).isEqualTo(Visibility.ALL);
    }

    @Test
    void aChangedSettingIsRememberedWithItsTime() {
        Result<Void> result = change.handle(new ChangeVisibilityCommand(player, Visibility.NONE));

        assertThat(result.isSuccess()).isTrue();
        assertThat(find.handle(new FindVisibilityQuery(player))).isEqualTo(Visibility.NONE);
        assertThat(repository.updatedAt(player)).contains(Instant.parse("2026-10-24T12:00:00Z"));
        assertThat(transactions.operations()).contains("visibility.change", "visibility.find");
    }

    @Test
    void theLatestChangeWins() {
        change.handle(new ChangeVisibilityCommand(player, Visibility.FRIENDS));
        change.handle(new ChangeVisibilityCommand(player, Visibility.ALL));

        assertThat(find.handle(new FindVisibilityQuery(player))).isEqualTo(Visibility.ALL);
    }
}
