package br.com.muxe.core.common.parkour.application.command;

import br.com.muxe.core.common.cqrs.command.CommandHandler;
import br.com.muxe.core.common.parkour.domain.ParkourOutcome;
import br.com.muxe.core.common.parkour.domain.ParkourRecordRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import br.com.muxe.core.common.result.Result;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

public final class SubmitParkourTimeHandler implements CommandHandler<SubmitParkourTimeCommand, Result<ParkourOutcome>> {

    private final ParkourRecordRepository records;
    private final Transactions transactions;
    private final Clock clock;

    public SubmitParkourTimeHandler(ParkourRecordRepository records, Transactions transactions, Clock clock) {
        this.records = records;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Result<ParkourOutcome> handle(SubmitParkourTimeCommand command) {
        return transactions.inTransaction("parkour.submit_time", () -> {
            Optional<Duration> previous = records.best(command.group(), command.playerId());
            boolean newRecord = previous.map(best -> command.time().compareTo(best) < 0).orElse(true);
            if (newRecord) {
                records.save(command.group(), command.playerId(), command.time(), clock.instant());
            }
            return Result.success(new ParkourOutcome(command.time(), previous, newRecord));
        });
    }
}
