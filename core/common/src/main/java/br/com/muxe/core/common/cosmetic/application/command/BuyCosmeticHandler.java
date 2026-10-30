package br.com.muxe.core.common.cosmetic.application.command;

import br.com.muxe.core.common.cosmetic.domain.CosmeticRepository;
import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.credit.application.command.ChangeCreditsHandler;
import br.com.muxe.core.common.credit.domain.CreditRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.time.Instant;
import java.util.OptionalLong;

public final class BuyCosmeticHandler implements CommandHandler<BuyCosmeticCommand, Result<Long>> {

    public static final String ALREADY_OWNED = "cosmetic.already_owned";
    public static final String SOURCE = "compra";

    private final CosmeticRepository cosmetics;
    private final CreditRepository credits;
    private final Transactions transactions;
    private final Clock clock;

    public BuyCosmeticHandler(CosmeticRepository cosmetics, CreditRepository credits, Transactions transactions, Clock clock) {
        this.cosmetics = cosmetics;
        this.credits = credits;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<Long> handle(BuyCosmeticCommand command) {
        return transactions.inTransaction("cosmetics.buy", () -> {
            if (cosmetics.owned(command.playerId(), command.category()).contains(command.cosmeticId())) {
                return Result.failure(ALREADY_OWNED, "The player already owns " + command.cosmeticId());
            }
            Instant now = clock.instant();
            OptionalLong left = command.price() == 0
                ? OptionalLong.of(credits.balance(command.playerId()))
                : credits.spend(command.playerId(), command.price(), "cosmetic:" + command.cosmeticId(), now);
            if (left.isEmpty()) {
                return Result.failure(ChangeCreditsHandler.NOT_ENOUGH, "The player cannot pay " + command.price() + " credits");
            }
            cosmetics.own(command.playerId(), command.category(), command.cosmeticId(), SOURCE, now);
            return Result.success(left.getAsLong());
        });
    }
}
