package br.com.muxe.core.common.cosmetic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.cosmetic.application.command.BuyCosmeticCommand;
import br.com.muxe.core.common.cosmetic.application.command.BuyCosmeticHandler;
import br.com.muxe.core.common.cosmetic.application.command.SelectCosmeticCommand;
import br.com.muxe.core.common.cosmetic.application.command.SelectCosmeticHandler;
import br.com.muxe.core.common.cosmetic.application.query.FindCosmeticsHandler;
import br.com.muxe.core.common.cosmetic.application.query.FindCosmeticsQuery;
import br.com.muxe.core.common.cosmetic.domain.CosmeticProfile;
import br.com.muxe.core.common.credit.application.command.ChangeCreditsCommand;
import br.com.muxe.core.common.credit.application.command.ChangeCreditsHandler;
import br.com.muxe.core.common.credit.application.query.FindCreditsHandler;
import br.com.muxe.core.common.credit.application.query.FindCreditsQuery;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryCosmeticRepository;
import br.com.muxe.core.common.testing.InMemoryCreditRepository;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CosmeticHandlersTest {

    private static final String BEDS = "destruicao-de-cama";

    private final InMemoryCreditRepository credits = new InMemoryCreditRepository();
    private final InMemoryCosmeticRepository cosmetics = new InMemoryCosmeticRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-30T21:00:00Z"));
    private final ChangeCreditsHandler change = new ChangeCreditsHandler(credits, transactions, clock);
    private final FindCreditsHandler balance = new FindCreditsHandler(credits, transactions);
    private final BuyCosmeticHandler buy = new BuyCosmeticHandler(cosmetics, credits, transactions, clock);
    private final SelectCosmeticHandler select = new SelectCosmeticHandler(cosmetics, transactions, clock);
    private final FindCosmeticsHandler find = new FindCosmeticsHandler(cosmetics, transactions);
    private final UUID player = UUID.randomUUID();

    @Test
    void creditsStartAtZeroGrowAndNeverGoNegative() {
        assertThat(balance.handle(new FindCreditsQuery(player))).isZero();
        assertThat(change.handle(new ChangeCreditsCommand(player, 1_000, "staff"))).isEqualTo(Result.success(1_000L));

        Result<Long> overdraw = change.handle(new ChangeCreditsCommand(player, -1_500, "staff"));

        assertThat(overdraw.isSuccess()).isFalse();
        assertThat(balance.handle(new FindCreditsQuery(player))).isEqualTo(1_000);
        assertThat(change.handle(new ChangeCreditsCommand(player, -400, "staff"))).isEqualTo(Result.success(600L));
        assertThat(credits.changes()).extracting(InMemoryCreditRepository.Change::amount).containsExactly(1_000L, -400L);
    }

    @Test
    void buyingSpendsTheCreditsAndOwnsTheCosmeticOnce() {
        change.handle(new ChangeCreditsCommand(player, 2_000, "staff"));

        assertThat(buy.handle(new BuyCosmeticCommand(player, BEDS, "tinta", 1_750))).isEqualTo(Result.success(250L));
        Result<Long> again = buy.handle(new BuyCosmeticCommand(player, BEDS, "tinta", 1_750));

        assertThat(again.isSuccess()).isFalse();
        assertThat(balance.handle(new FindCreditsQuery(player))).isEqualTo(250);
        assertThat(find.handle(new FindCosmeticsQuery(player, BEDS)).owns("tinta")).isTrue();
        assertThat(credits.changes().getLast().reason()).isEqualTo("cosmetic:tinta");
    }

    @Test
    void withoutEnoughCreditsNothingIsBoughtOrSpent() {
        change.handle(new ChangeCreditsCommand(player, 15, "staff"));

        Result<Long> result = buy.handle(new BuyCosmeticCommand(player, BEDS, "enderman", 1_750));

        assertThat(result.isSuccess()).isFalse();
        assertThat(balance.handle(new FindCreditsQuery(player))).isEqualTo(15);
        assertThat(find.handle(new FindCosmeticsQuery(player, BEDS))).isEqualTo(CosmeticProfile.EMPTY);
    }

    @Test
    void theSelectionIsRememberedPerCategory() {
        select.handle(new SelectCosmeticCommand(player, BEDS, "aleatorio"));
        select.handle(new SelectCosmeticCommand(player, BEDS, "friaca"));

        assertThat(find.handle(new FindCosmeticsQuery(player, BEDS)).selected()).isEqualTo(Optional.of("friaca"));
        assertThat(find.handle(new FindCosmeticsQuery(player, "kill-final")).selected()).isEmpty();
        assertThat(transactions.operations()).contains("cosmetics.select", "cosmetics.find");
    }

    @Test
    void idsAndReasonsAreValidated() {
        assertThatThrownBy(() -> new BuyCosmeticCommand(player, "Camas", "x", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChangeCreditsCommand(player, 0, "staff")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChangeCreditsCommand(player, 5, "has space")).isInstanceOf(IllegalArgumentException.class);
    }
}
