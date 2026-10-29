package br.com.muxe.core.common.parkour.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.parkour.application.command.SubmitParkourTimeCommand;
import br.com.muxe.core.common.parkour.application.command.SubmitParkourTimeHandler;
import br.com.muxe.core.common.parkour.application.query.FindParkourRecordHandler;
import br.com.muxe.core.common.parkour.application.query.FindParkourRecordQuery;
import br.com.muxe.core.common.parkour.domain.ParkourOutcome;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryParkourRecordRepository;
import br.com.muxe.core.common.testing.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkourHandlersTest {

    private final InMemoryParkourRecordRepository records = new InMemoryParkourRecordRepository();
    private final DirectTransactions transactions = new DirectTransactions();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-27T21:00:00Z"));
    private final SubmitParkourTimeHandler submit = new SubmitParkourTimeHandler(records, transactions, clock);
    private final FindParkourRecordHandler find = new FindParkourRecordHandler(records, transactions);
    private final UUID player = UUID.randomUUID();

    @Test
    void theFirstFinishIsTheRecord() {
        ParkourOutcome outcome = submit.handle(new SubmitParkourTimeCommand("lobby", player, Duration.ofMillis(298_902))).orElseThrow();

        assertThat(outcome).isEqualTo(new ParkourOutcome(Duration.ofMillis(298_902), Optional.empty(), true));
        assertThat(find.handle(new FindParkourRecordQuery("lobby", player))).contains(Duration.ofMillis(298_902));
        assertThat(transactions.operations()).contains("parkour.submit_time", "parkour.find_record");
    }

    @Test
    void aFasterFinishBeatsTheRecordAndASlowerOneKeepsIt() {
        submit.handle(new SubmitParkourTimeCommand("lobby", player, Duration.ofMillis(298_902)));

        ParkourOutcome faster = submit.handle(new SubmitParkourTimeCommand("lobby", player, Duration.ofMillis(194_002))).orElseThrow();
        ParkourOutcome slower = submit.handle(new SubmitParkourTimeCommand("lobby", player, Duration.ofMinutes(10))).orElseThrow();

        assertThat(faster).isEqualTo(new ParkourOutcome(Duration.ofMillis(194_002), Optional.of(Duration.ofMillis(298_902)), true));
        assertThat(slower.newRecord()).isFalse();
        assertThat(slower.best()).isEqualTo(Duration.ofMillis(194_002));
        assertThat(find.handle(new FindParkourRecordQuery("lobby", player))).contains(Duration.ofMillis(194_002));
    }

    @Test
    void eachMapHasItsOwnRecord() {
        submit.handle(new SubmitParkourTimeCommand("lobby", player, Duration.ofMillis(298_902)));

        ParkourOutcome bedwars = submit.handle(new SubmitParkourTimeCommand("bedwars-lobby", player, Duration.ofMinutes(9))).orElseThrow();

        assertThat(bedwars.newRecord()).isTrue();
        assertThat(bedwars.previous()).isEmpty();
        assertThat(find.handle(new FindParkourRecordQuery("lobby", player))).contains(Duration.ofMillis(298_902));
        assertThat(find.handle(new FindParkourRecordQuery("bedwars-lobby", UUID.randomUUID()))).isEmpty();
    }
}
