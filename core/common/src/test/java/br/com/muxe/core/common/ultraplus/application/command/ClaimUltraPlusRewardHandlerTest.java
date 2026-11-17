package br.com.muxe.core.common.ultraplus.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryCreditRepository;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.common.ultraplus.domain.InMemoryUltraPlusRepository;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusStyle;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClaimUltraPlusRewardHandlerTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private final InMemoryUltraPlusRepository profiles = new InMemoryUltraPlusRepository();
    private final InMemoryCreditRepository credits = new InMemoryCreditRepository();
    private final MutableClock clock = MutableClock.startingAt("2026-11-17T12:00:00Z");
    private final ClaimUltraPlusRewardHandler claim = new ClaimUltraPlusRewardHandler(profiles, credits, new DirectTransactions(), clock);
    private final UUID player = UUID.randomUUID();

    @Test
    void theRewardPaysTheCreditsOncePerMonth() {
        Result<Void> first = claim.handle(new ClaimUltraPlusRewardCommand(player, NOVEMBER, 450));
        Result<Void> again = claim.handle(new ClaimUltraPlusRewardCommand(player, NOVEMBER, 450));
        Result<Void> december = claim.handle(new ClaimUltraPlusRewardCommand(player, NOVEMBER.plusMonths(1), 450));

        assertThat(first.isSuccess()).isTrue();
        assertThat(again).isInstanceOfSatisfying(Result.Failure.class,
            failure -> assertThat(failure.problem().code()).isEqualTo(ClaimUltraPlusRewardHandler.ALREADY_CLAIMED));
        assertThat(december.isSuccess()).isTrue();
        assertThat(credits.balance(player)).isEqualTo(900);
        assertThat(profiles.find(player).orElseThrow().claimed(NOVEMBER.plusMonths(1))).isTrue();
    }

    @Test
    void changingTheStyleKeepsTheGrantedMonthsAndTheClaim() {
        claim.handle(new ClaimUltraPlusRewardCommand(player, NOVEMBER, 450));
        UltraPlusStyle style = new UltraPlusStyle(Optional.of("flor"), Optional.of("amarelo"), 2);

        new ChangeUltraPlusStyleHandler(profiles, new DirectTransactions(), clock).handle(new ChangeUltraPlusStyleCommand(player, style));

        assertThat(profiles.find(player).orElseThrow().style()).isEqualTo(style);
        assertThat(profiles.find(player).orElseThrow().claimed(NOVEMBER)).isTrue();
    }
}
