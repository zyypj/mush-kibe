package br.com.muxe.core.proxy.geoip.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.account.AccountDetailsModule;
import br.com.muxe.core.common.account.application.query.FindAccountDetailsQuery;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountDetails;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.cqrs.bus.SimpleCommandBus;
import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryAccountDetailsRepository;
import br.com.muxe.core.common.testing.MutableClock;
import br.com.muxe.core.proxy.geoip.lookup.GeoLocator;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class LocationRecorderTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final AccountLocation CAMPINAS = new AccountLocation(
        Optional.of("Brasil"), Optional.of("São Paulo"), Optional.of("Campinas"), Optional.of("AS28573 Claro"));

    private final InMemoryAccountDetailsRepository accounts = new InMemoryAccountDetailsRepository();
    private final SimpleCommandBus commands = new SimpleCommandBus(Runnable::run, List.of());
    private final SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
    private final List<InetAddress> lookedUp = new ArrayList<>();
    private final List<String> logs = new ArrayList<>();
    private final UUID player = UUID.randomUUID();

    LocationRecorderTest() {
        AccountDetailsModule.register(commands, queries, accounts, new DirectTransactions(), new MutableClock(NOW));
        accounts.add(Account.firstLogin(player, "tadeu", false, NOW));
    }

    @Test
    void theLocationOfAPublicAddressIsSaved() throws IOException {
        LocationRecorder recorder = recorder(address -> {
            lookedUp.add(address);
            return CAMPINAS;
        });

        assertThat(recorder.record(player, InetAddress.getByName("151.244.40.191")).join()).isTrue();

        assertThat(details().location()).contains(CAMPINAS);
    }

    @Test
    void privateAddressesAreNeverLookedUp() throws IOException {
        LocationRecorder recorder = recorder(address -> {
            lookedUp.add(address);
            return CAMPINAS;
        });

        assertThat(recorder.record(player, InetAddress.getByName("192.168.0.20")).join()).isFalse();

        assertThat(lookedUp).isEmpty();
        assertThat(details().location()).isEmpty();
    }

    @Test
    void anUnknownLocationIsNotSaved() throws IOException {
        LocationRecorder recorder = recorder(GeoLocator.NONE);

        assertThat(recorder.record(player, InetAddress.getByName("8.8.8.8")).join()).isFalse();

        assertThat(details().location()).isEmpty();
    }

    @Test
    void aFailedLookupIsLoggedWithoutTheAddress() throws IOException {
        LocationRecorder recorder = recorder(address -> {
            throw new IOException("lookup of " + address.getHostAddress() + " failed");
        });

        assertThat(recorder.record(player, InetAddress.getByName("8.8.4.4")).join()).isFalse();

        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst()).contains(player.toString()).doesNotContain("8.8.4.4");
    }

    private LocationRecorder recorder(GeoLocator locator) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record.getMessage() + (record.getThrown() == null ? "" : " " + record.getThrown()));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return new LocationRecorder(commands, () -> locator, Runnable::run, logger);
    }

    private AccountDetails details() {
        return queries.ask(FindAccountDetailsQuery.byId(player)).join().orElseThrow();
    }
}
