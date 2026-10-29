package br.com.muxe.core.common.parkour.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.parkour.domain.ParkourRecordRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;
import java.time.Duration;
import java.util.Optional;

public final class FindParkourRecordHandler implements QueryHandler<FindParkourRecordQuery, Optional<Duration>> {

    private final ParkourRecordRepository records;
    private final Transactions transactions;

    public FindParkourRecordHandler(ParkourRecordRepository records, Transactions transactions) {
        this.records = records;
        this.transactions = transactions;
    }

    @Override
    public Optional<Duration> handle(FindParkourRecordQuery query) {
        return transactions.readOnly("parkour.find_record", () -> records.best(query.group(), query.playerId()));
    }
}
