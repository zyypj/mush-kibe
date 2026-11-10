package br.com.muxe.core.common.language.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.language.application.command.ChangeLanguageCommand;
import br.com.muxe.core.common.language.application.command.ChangeLanguageHandler;
import br.com.muxe.core.common.language.application.query.FindLanguageHandler;
import br.com.muxe.core.common.language.application.query.FindLanguageQuery;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryLanguageRepository;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LanguageHandlersTest {

    private final InMemoryLanguageRepository repository = new InMemoryLanguageRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-10T12:00:00Z"));
    private final ChangeLanguageHandler change = new ChangeLanguageHandler(repository, transactions, clock);
    private final FindLanguageHandler find = new FindLanguageHandler(repository, transactions);
    private final UUID player = UUID.randomUUID();

    @Test
    void newPlayersSpeakPortuguese() {
        assertThat(find.handle(new FindLanguageQuery(player))).isEqualTo(Language.PORTUGUESE);
    }

    @Test
    void aChosenLanguageIsRememberedWithItsTime() {
        Result<Void> result = change.handle(new ChangeLanguageCommand(player, Language.ENGLISH));

        assertThat(result.isSuccess()).isTrue();
        assertThat(find.handle(new FindLanguageQuery(player))).isEqualTo(Language.ENGLISH);
        assertThat(repository.updatedAt(player)).contains(Instant.parse("2026-11-10T12:00:00Z"));
        assertThat(transactions.operations()).contains("language.change", "language.find");
    }

    @Test
    void theLatestChoiceWins() {
        change.handle(new ChangeLanguageCommand(player, Language.ENGLISH));
        change.handle(new ChangeLanguageCommand(player, Language.PORTUGUESE));

        assertThat(find.handle(new FindLanguageQuery(player))).isEqualTo(Language.PORTUGUESE);
    }
}
