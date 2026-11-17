package br.com.muxe.core.common.ultraplus.application.command;

import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusProfile;
import br.com.muxe.core.common.ultraplus.domain.UltraPlusRepository;
import java.time.Clock;
import java.time.Instant;

public final class ClaimUltraPlusRewardHandler implements CommandHandler<ClaimUltraPlusRewardCommand, Result<Void>> {

    public static final String ALREADY_CLAIMED = "ultraplus.already-claimed";
    public static final String REASON = "ultra-plus-monthly-reward";

    private final UltraPlusRepository profiles;
    private final CreditRepository credits;
    private final Transactions transactions;
    private final Clock clock;

    public ClaimUltraPlusRewardHandler(UltraPlusRepository profiles, CreditRepository credits, Transactions transactions, Clock clock) {
        this.profiles = profiles;
        this.credits = credits;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ClaimUltraPlusRewardCommand command) {
        return transactions.inTransaction("ultraplus.claim_reward", () -> {
            UltraPlusProfile profile = profiles.find(command.playerId()).orElseGet(() -> UltraPlusProfile.empty(command.playerId()));
            if (profile.claimed(command.month())) {
                return Result.<Void>failure(ALREADY_CLAIMED, "The reward of " + command.month() + " was already claimed");
            }
            Instant now = clock.instant();
            profiles.save(profile.claim(command.month()), now);
            credits.add(command.playerId(), command.credits(), REASON, now);
            return Result.success();
        });
    }
}
