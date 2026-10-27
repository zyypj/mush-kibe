package br.com.muxe.core.common.parkour;

import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.common.parkour.application.command.SubmitParkourTimeCommand;
import br.com.muxe.core.common.parkour.application.command.SubmitParkourTimeHandler;
import br.com.muxe.core.common.parkour.application.query.FindParkourRecordHandler;
import br.com.muxe.core.common.parkour.application.query.FindParkourRecordQuery;
import br.com.muxe.core.common.parkour.domain.ParkourRecordRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Clock;

public final class ParkourModule {

    private ParkourModule() {
    }

    public static void register(CommandBus commands, QueryBus queries, ParkourRecordRepository records, Transactions transactions, Clock clock) {
        commands.register(SubmitParkourTimeCommand.class, new SubmitParkourTimeHandler(records, transactions, clock));
        queries.register(FindParkourRecordQuery.class, new FindParkourRecordHandler(records, transactions));
    }
}
